package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Session;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;

/**
 * Harvests ripe crops around the player (or only the one under the crosshair) and plants the
 * seeds again. Wheat, carrots, potatoes, beetroots and nether wart are replanted; cocoa and sweet
 * berries are harvested; crops from other mods are harvested when they are ripe.
 */
public class AutoFarmModule extends Module {
    /** Ticks to wait for the harvested block to clear before giving up the replanting. */
    private static final int REPLANT_TIMEOUT = 10;
    /** Vanilla lets a survival player reach blocks this far from the eyes. */
    private static final double MAX_REACH = 4.5;

    public final IntSetting range = add(new IntSetting("range", 3, 0, 4, Unit.BLOCKS)
            .zeroMeans("multiclicker.value.crosshair"));
    public final BoolSetting replant = add(new BoolSetting("replant", true));
    public final IntSetting delay = add(new IntSetting("delay", 2, 1, 20, Unit.TICKS));

    private int cooldown;
    @Nullable
    private BlockPos replantPos;
    @Nullable
    private Item replantSeed;
    private int replantTimer;
    private int harvested;

    public AutoFarmModule() {
        super("auto_farm", Category.AUTOMATION, true, false);
    }

    @Override
    public void start(Minecraft mc) {
        cooldown = 0;
        harvested = 0;
        replantPos = null;
    }

    @Override
    public void stop(Minecraft mc) {
        replantPos = null;
    }

    @Override
    public void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (Screens.current(mc) != null || player.isSpectator() || player.isUsingItem() || mc.gameMode.isDestroying()
                || mod.autoEat().isBusy() || mod.clicker().isSwappingWeapon()) {
            return;
        }
        if (replantPos != null && tryReplant(mc, player)) {
            cooldown = delay.get();
            return;
        }
        BlockPos target = findRipe(mc, player);
        if (target != null) {
            harvest(mc, player, target);
            cooldown = delay.get();
        }
    }

    /** Whether the block is a crop ready to be harvested. */
    public static boolean isRipe(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        return false;
    }

    /** What to plant where the crop was, or {@code null} if it is not replanted. */
    @Nullable
    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT) {
            return Items.WHEAT_SEEDS;
        }
        if (block == Blocks.CARROTS) {
            return Items.CARROT;
        }
        if (block == Blocks.POTATOES) {
            return Items.POTATO;
        }
        if (block == Blocks.BEETROOTS) {
            return Items.BEETROOT_SEEDS;
        }
        if (block == Blocks.NETHER_WART) {
            return Items.NETHER_WART;
        }
        return null;
    }

    @Nullable
    private BlockPos findRipe(Minecraft mc, LocalPlayer player) {
        int radius = range.get();
        if (radius == 0) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    && isRipe(mc.level.getBlockState(hit.getBlockPos()))) {
                return hit.getBlockPos();
            }
            return null;
        }
        Vec3 eyes = player.getEyePosition();
        double reach = Math.min(radius + 1.0, MAX_REACH);
        BlockPos feet = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, 1, radius))) {
            double distance = eyes.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance && distance <= reach * reach && isRipe(mc.level.getBlockState(pos))) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private void harvest(Minecraft mc, LocalPlayer player, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            // Berries are picked with the use key; bone meal in the hand would grow the bush instead.
            InteractionHand hand = player.getMainHandItem().is(Items.BONE_MEAL) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItemOn(player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        } else {
            mc.gameMode.startDestroyBlock(pos, Direction.UP);
            Item seed = seedFor(state.getBlock());
            if (replant.get() && seed != null) {
                replantPos = pos;
                replantSeed = seed;
                replantTimer = 0;
            }
        }
        Session.swing(player);
        harvested++;
        MultiClicker.get().stats().count(Stat.CROPS);
    }

    /**
     * Plants the seed where the crop was: from the offhand, or from the hotbar, switching back to
     * the slot in use right after.
     *
     * @return whether it acted this tick
     */
    private boolean tryReplant(Minecraft mc, LocalPlayer player) {
        BlockPos pos = replantPos;
        if (++replantTimer > REPLANT_TIMEOUT) {
            replantPos = null;
            return false;
        }
        if (!mc.level.getBlockState(pos).isAir()) {
            return false; // the harvest is not through yet
        }
        InteractionHand hand;
        int previous = -1;
        if (player.getOffhandItem().is(replantSeed)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            int slot = findHotbar(player, replantSeed);
            if (slot == -1) {
                replantPos = null;
                return false;
            }
            hand = InteractionHand.MAIN_HAND;
            int selected = Slots.selected(player.getInventory());
            if (slot != selected) {
                previous = selected;
                Inventories.selectSlot(player, slot);
            }
        }
        BlockPos soil = pos.below();
        mc.gameMode.useItemOn(player, hand, new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
        if (previous != -1) {
            Inventories.selectSlot(player, previous);
        }
        replantPos = null;
        return true;
    }

    private static int findHotbar(LocalPlayer player, Item item) {
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public String hudInfo() {
        return harvested > 0 ? Integer.toString(harvested) : null;
    }
}
