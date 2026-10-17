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

import java.util.function.Predicate;

/**
 * Harvests ripe crops around the player (or only the one under the crosshair) and plants the
 * seeds again. Wheat, carrots, potatoes, beetroots and nether wart are replanted; cocoa and sweet
 * berries are harvested; crops from other mods are harvested when they are ripe. With bone meal on,
 * a growing crop in range is fertilized while nothing is ripe.
 */
public class AutoFarmModule extends Module {
    /** Ticks to wait for the harvested block to clear before giving up the replanting. */
    private static final int REPLANT_TIMEOUT = 10;
    /** Vanilla lets a survival player reach blocks this far from the eyes. */
    private static final double MAX_REACH = 4.5;

    public final IntSetting range = add(new IntSetting("range", 3, 0, 4, Unit.BLOCKS)
            .zeroMeans("multiclicker.value.crosshair"));
    public final BoolSetting replant = add(new BoolSetting("replant", true));
    public final BoolSetting boneMeal = add(new BoolSetting("bone_meal", false));
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
    public void resume(Minecraft mc) {
        int count = harvested;
        start(mc);
        harvested = count;
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
                || mod.isConsuming() || mod.clicker().isSwappingWeapon()) {
            return;
        }
        if (replantPos != null && tryReplant(mc, player)) {
            cooldown = delay.get();
            return;
        }
        BlockPos target = find(mc, player, AutoFarmModule::isRipe);
        if (target != null) {
            harvest(mc, player, target);
            cooldown = delay.get();
            return;
        }
        if (boneMeal.get() && hasItem(player, Items.BONE_MEAL)) {
            target = find(mc, player, AutoFarmModule::isGrowing);
            if (target != null && useOn(mc, player, Items.BONE_MEAL, new BlockHitResult(Vec3.atCenterOf(target),
                    Direction.UP, target, false))) {
                Session.swing(player);
                cooldown = delay.get();
            }
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

    /** Whether the block is a vanilla crop that bone meal makes grow further. */
    public static boolean isGrowing(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) < 2;
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
    private BlockPos find(Minecraft mc, LocalPlayer player, Predicate<BlockState> wanted) {
        int radius = range.get();
        if (radius == 0) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    && wanted.test(mc.level.getBlockState(hit.getBlockPos()))) {
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
            if (distance < bestDistance && distance <= reach * reach && wanted.test(mc.level.getBlockState(pos))) {
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
     * Plants the seed where the crop was, once the harvest is through.
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
        BlockPos soil = pos.below();
        boolean planted = useOn(mc, player, replantSeed,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
        replantPos = null;
        return planted;
    }

    /**
     * Uses an item on a block: from the offhand, from the hotbar (switching back to the slot in use
     * right after) or from the main inventory (swapped into the hand and back again).
     *
     * @return whether the item was found and used
     */
    private static boolean useOn(Minecraft mc, LocalPlayer player, Item item, BlockHitResult hit) {
        InteractionHand hand;
        int previous = -1;
        int borrowed = -1;
        int selected = Slots.selected(player.getInventory());
        if (player.getOffhandItem().is(item)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            hand = InteractionHand.MAIN_HAND;
            int slot = findHotbar(player, item);
            if (slot != -1) {
                if (slot != selected) {
                    previous = selected;
                    Inventories.selectSlot(player, slot);
                }
            } else {
                borrowed = findMain(player, item);
                if (borrowed == -1 || !Inventories.canClickInventory(mc)) {
                    return false;
                }
                // The held item goes back in the same tick, so its attack charge is kept.
                Inventories.swapWithHotbar(mc, borrowed, selected);
            }
        }
        mc.gameMode.useItemOn(player, hand, hit);
        if (previous != -1) {
            Inventories.selectSlot(player, previous);
        }
        if (borrowed != -1) {
            Inventories.swapWithHotbar(mc, borrowed, selected);
        }
        return true;
    }

    private static boolean hasItem(LocalPlayer player, Item item) {
        return player.getOffhandItem().is(item) || findHotbar(player, item) != -1 || findMain(player, item) != -1;
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

    private static int findMain(LocalPlayer player, Item item) {
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
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
