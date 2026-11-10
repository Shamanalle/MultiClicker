package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
 * seeds again. Wheat, carrots, potatoes, beetroots, nether wart and cocoa are replanted; sweet
 * berries are picked; sugar cane, bamboo and kelp are cut above their root block; crops with
 * non-zero hardness are mined continuously until broken.
 */
public class AutoFarmModule extends Module {
    /** Ticks to wait for the harvested block to clear before giving up the replanting. */
    private static final int REPLANT_TIMEOUT = 10;
    /** Safety cap on continuous block breaking for tough crops such as bamboo by hand. */
    private static final int BREAK_TIMEOUT = 100;
    /** Vanilla lets a survival player reach blocks this far from the eyes. */
    private static final double MAX_REACH = 4.5;
    /** Cosine of ~75 degrees: natural field-of-view cone when {@link #onlyInView} is enabled. */
    private static final double MIN_VIEW_DOT = 0.25;

    public final IntSetting range = add(new IntSetting("range", 3, 0, 4, Unit.BLOCKS)
            .zeroMeans("multiclicker.value.crosshair"));
    public final BoolSetting replant = add(new BoolSetting("replant", true));
    public final BoolSetting tallCrops = add(new BoolSetting("tall_crops", true));
    public final BoolSetting boneMeal = add(new BoolSetting("bone_meal", false));
    public final BoolSetting lineOfSight = add(new BoolSetting("line_of_sight", true));
    public final BoolSetting onlyInView = add(new BoolSetting("only_in_view", false));
    public final IntSetting delay = add(new IntSetting("delay", 2, 1, 20, Unit.TICKS));

    private int cooldown;
    @Nullable
    private BlockPos breakingPos;
    @Nullable
    private BlockState breakingState;
    private int breakingTicks;
    @Nullable
    private BlockPos replantPos;
    @Nullable
    private Item replantSeed;
    @Nullable
    private Direction replantFacing;
    private int replantTimer;
    private int borrowedInvSlot = -1;
    private int borrowedHotbarSlot = -1;
    private int harvested;

    public AutoFarmModule() {
        super("auto_farm", Category.AUTOMATION, true, false);
    }

    @Override
    public void start(Minecraft mc) {
        cooldown = 0;
        harvested = 0;
        breakingPos = null;
        breakingState = null;
        breakingTicks = 0;
        replantPos = null;
        replantSeed = null;
        replantFacing = null;
        borrowedInvSlot = -1;
        borrowedHotbarSlot = -1;
    }

    @Override
    public void resume(Minecraft mc) {
        int count = harvested;
        start(mc);
        harvested = count;
    }

    @Override
    public void stop(Minecraft mc) {
        cancelBreaking(mc);
        returnBorrowed(mc);
        replantPos = null;
        replantSeed = null;
        replantFacing = null;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (borrowedInvSlot != -1) {
            if (Inventories.canClickMainInventory(mc)) {
                returnBorrowed(mc);
            }
            return;
        }
        if (breakingPos != null) {
            tickBreaking(mc, player, mod);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (Screens.current(mc) != null || player.isSpectator() || player.isUsingItem() || mc.gameMode.isDestroying()
                || mod.isConsuming() || mod.clicker().isSwappingWeapon()) {
            return;
        }
        if (replantPos != null && tryReplant(mc, player)) {
            cooldown = delay.get();
            return;
        }
        BlockPos target = findHarvest(mc, player);
        if (target != null) {
            harvest(mc, player, target);
            return;
        }
        if (boneMeal.get() && hasItem(player, Items.BONE_MEAL)) {
            target = findGrowing(mc, player);
            if (target != null && useOn(mc, player, Items.BONE_MEAL, new BlockHitResult(Vec3.atCenterOf(target),
                    Direction.UP, target, false))) {
                Session.swing(player);
                cooldown = delay.get();
            }
        }
    }

    private void tickBreaking(Minecraft mc, LocalPlayer player, MultiClicker mod) {
        if (Screens.current(mc) != null || player.isSpectator() || player.isUsingItem()
                || mod.isConsuming() || mod.clicker().isSwappingWeapon()) {
            cancelBreaking(mc);
            return;
        }
        BlockPos pos = breakingPos;
        BlockState expected = breakingState;
        if (isBroken(expected, mc.level.getBlockState(pos))) {
            breakingPos = null;
            breakingState = null;
            completeHarvest(pos, expected);
            cooldown = delay.get();
            return;
        }
        if (++breakingTicks > BREAK_TIMEOUT || !inReachAndVisible(mc, player, player.getEyePosition(), pos, MAX_REACH)) {
            cancelBreaking(mc);
            return;
        }
        mc.gameMode.continueDestroyBlock(pos, Direction.UP);
        Session.swing(player);
        if (isBroken(expected, mc.level.getBlockState(pos))) {
            breakingPos = null;
            breakingState = null;
            completeHarvest(pos, expected);
            cooldown = delay.get();
        }
    }

    private void cancelBreaking(Minecraft mc) {
        if (breakingPos != null) {
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
            breakingPos = null;
            breakingState = null;
            breakingTicks = 0;
        }
    }

    private void returnBorrowed(Minecraft mc) {
        if (borrowedInvSlot != -1) {
            if (Inventories.canClickInventory(mc)) {
                Inventories.swapWithHotbar(mc, borrowedInvSlot, borrowedHotbarSlot);
            }
            borrowedInvSlot = -1;
            borrowedHotbarSlot = -1;
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

    /** Whether the block at {@code pos} is a tall plant segment above its bottom root block. */
    public static boolean isHarvestableTallCrop(Level level, BlockPos pos, BlockState state) {
        return isTallPlant(state) && isSameTallPlant(state, level.getBlockState(pos.below()));
    }

    private static boolean isTallPlant(BlockState state) {
        return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || isKelp(state);
    }

    private static boolean isKelp(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    private static boolean isSameTallPlant(BlockState a, BlockState b) {
        if (isKelp(a)) {
            return isKelp(b);
        }
        return a.getBlock() == b.getBlock();
    }

    private static boolean isBroken(BlockState original, BlockState now) {
        if (now.isAir()) {
            return true;
        }
        if (isKelp(original)) {
            return !isKelp(now);
        }
        return now.getBlock() != original.getBlock();
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
        if (block == Blocks.COCOA) {
            return Items.COCOA_BEANS;
        }
        return null;
    }

    @Nullable
    private BlockPos findHarvest(Minecraft mc, LocalPlayer player) {
        int radius = range.get();
        Level level = mc.level;
        if (radius == 0) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (isRipe(state) || tallCrops.get() && isHarvestableTallCrop(level, pos, state)) {
                    return pos;
                }
            }
            return null;
        }
        Vec3 eyes = player.getEyePosition();
        double reach = Math.min(radius + 1.0, MAX_REACH);
        BlockPos feet = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, 2, radius))) {
            if (!inReachAndVisible(mc, player, eyes, pos, reach)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean match = isRipe(state);
            if (!match && tallCrops.get() && isHarvestableTallCrop(level, pos, state)) {
                BlockPos below = pos.below();
                // Prefer cutting the second block from the bottom so the whole stalk drops in one swing.
                boolean lowerSegmentAvailable = isHarvestableTallCrop(level, below, level.getBlockState(below))
                        && inReachAndVisible(mc, player, eyes, below, reach);
                match = !lowerSegmentAvailable;
            }
            if (!match) {
                continue;
            }
            double distance = eyes.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findGrowing(Minecraft mc, LocalPlayer player) {
        int radius = range.get();
        if (radius == 0) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    && isGrowing(mc.level.getBlockState(hit.getBlockPos()))) {
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
            if (!inReachAndVisible(mc, player, eyes, pos, reach) || !isGrowing(mc.level.getBlockState(pos))) {
                continue;
            }
            double distance = eyes.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean inReachAndVisible(Minecraft mc, LocalPlayer player, Vec3 eyes, BlockPos pos, double reach) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 delta = center.subtract(eyes);
        double distSqr = delta.lengthSqr();
        if (distSqr > reach * reach) {
            return false;
        }
        if (onlyInView.get() && distSqr > 1.0E-4) {
            Vec3 dir = delta.scale(1.0 / Math.sqrt(distSqr));
            if (player.getViewVector(1.0F).dot(dir) < MIN_VIEW_DOT) {
                return false;
            }
        }
        if (lineOfSight.get()) {
            BlockHitResult clip = mc.level.clip(new ClipContext(
                    eyes, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (clip.getType() != HitResult.Type.MISS && !clip.getBlockPos().equals(pos)) {
                return false;
            }
        }
        return true;
    }

    private void harvest(Minecraft mc, LocalPlayer player, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            // Berries are picked with the use key; bone meal in the hand would grow the bush instead.
            InteractionHand hand = player.getMainHandItem().is(Items.BONE_MEAL) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItemOn(player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            Session.swing(player);
            harvested++;
            MultiClicker.get().stats().count(Stat.CROPS);
            cooldown = delay.get();
            return;
        }
        mc.gameMode.startDestroyBlock(pos, Direction.UP);
        Session.swing(player);
        if (isBroken(state, mc.level.getBlockState(pos))) {
            completeHarvest(pos, state);
            cooldown = delay.get();
        } else {
            breakingPos = pos.immutable();
            breakingState = state;
            breakingTicks = 0;
        }
    }

    private void completeHarvest(BlockPos pos, BlockState state) {
        harvested++;
        MultiClicker.get().stats().count(Stat.CROPS);
        Item seed = seedFor(state.getBlock());
        if (replant.get() && seed != null) {
            replantPos = pos.immutable();
            replantSeed = seed;
            replantFacing = state.getBlock() instanceof CocoaBlock ? state.getValue(CocoaBlock.FACING) : null;
            replantTimer = 0;
        }
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
            replantFacing = null;
            return false;
        }
        if (!mc.level.getBlockState(pos).isAir()) {
            return false; // the harvest is not through yet
        }
        BlockHitResult hit;
        if (replantFacing != null) {
            BlockPos log = pos.relative(replantFacing);
            Direction face = replantFacing.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(log).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            hit = new BlockHitResult(hitVec, face, log, false);
        } else {
            BlockPos soil = pos.below();
            hit = new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false);
        }
        boolean planted = useOn(mc, player, replantSeed, hit);
        replantPos = null;
        replantFacing = null;
        return planted;
    }

    /**
     * Uses an item on a block: from the offhand, from the hotbar (switching back to the slot in use
     * right after) or from the main inventory (moved into an empty hotbar slot if available, or
     * borrowed cleanly).
     *
     * @return whether the item was found and used
     */
    private boolean useOn(Minecraft mc, LocalPlayer player, Item item, BlockHitResult hit) {
        InteractionHand hand;
        int previous = -1;
        int immediateBorrow = -1;
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
                int mainSlot = findMain(player, item);
                if (mainSlot == -1 || !Inventories.canClickMainInventory(mc)) {
                    return false;
                }
                int emptyHotbar = findEmptyHotbar(player);
                if (emptyHotbar != -1) {
                    Inventories.swapWithHotbar(mc, mainSlot, emptyHotbar);
                    if (emptyHotbar != selected) {
                        previous = selected;
                        Inventories.selectSlot(player, emptyHotbar);
                    }
                } else if (MultiClicker.get().clicker().safeInventory.get()) {
                    int tempHotbar = (selected + 1) % Inventories.HOTBAR_SIZE;
                    Inventories.swapWithHotbar(mc, mainSlot, tempHotbar);
                    previous = selected;
                    Inventories.selectSlot(player, tempHotbar);
                    borrowedInvSlot = mainSlot;
                    borrowedHotbarSlot = tempHotbar;
                } else {
                    // The held item goes back in the same tick, so its attack charge is kept.
                    immediateBorrow = mainSlot;
                    Inventories.swapWithHotbar(mc, mainSlot, selected);
                }
            }
        }
        mc.gameMode.useItemOn(player, hand, hit);
        if (previous != -1) {
            Inventories.selectSlot(player, previous);
        }
        if (immediateBorrow != -1) {
            Inventories.swapWithHotbar(mc, immediateBorrow, selected);
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

    private static int findEmptyHotbar(LocalPlayer player) {
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
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
