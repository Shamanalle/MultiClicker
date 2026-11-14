package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
    /** Ticks to wait for the harvested block to clear and seeds to be picked up before giving up replanting. */
    private static final int REPLANT_TIMEOUT = 15;
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

    /** True while breaking a multi-tick crop block so vanilla does not abort the mining animation. */
    public boolean isBreakingBlock() {
        return isRunning() && breakingPos != null;
    }

    public boolean ownsSlot(int slot) {
        return slot >= 0 && (slot == borrowedInvSlot || slot == borrowedHotbarSlot);
    }

    public boolean hasPendingReturn() {
        return borrowedInvSlot != -1;
    }

    public void flushPendingReturn(Minecraft mc) {
        if (borrowedInvSlot != -1) {
            returnBorrowed(mc, false);
        }
    }

    public void clearBorrowedState() {
        borrowedInvSlot = -1;
        borrowedHotbarSlot = -1;
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
        returnBorrowed(mc, true);
        replantPos = null;
        replantSeed = null;
        replantFacing = null;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (borrowedInvSlot != -1) {
            returnBorrowed(mc, false);
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
        if (!mc.gameMode.isDestroying()) {
            breakingPos = null;
            breakingState = null;
            breakingTicks = 0;
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

    private void returnBorrowed(Minecraft mc, boolean immediate) {
        if (borrowedInvSlot != -1) {
            if (immediate ? Inventories.canClickInventory(mc) : Inventories.requestMainInventoryClick(mc)) {
                Inventories.swapWithHotbar(mc, borrowedInvSlot, borrowedHotbarSlot);
                borrowedInvSlot = -1;
                borrowedHotbarSlot = -1;
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
                if (canHarvestBlock(player, level, pos, state)) {
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
            boolean match = canHarvestRipe(player, state);
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

    private boolean canHarvestBlock(LocalPlayer player, Level level, BlockPos pos, BlockState state) {
        return canHarvestRipe(player, state) || tallCrops.get() && isHarvestableTallCrop(level, pos, state);
    }

    private boolean canHarvestRipe(LocalPlayer player, BlockState state) {
        if (!isRipe(state)) {
            return false;
        }
        if (replantPos != null && seedFor(state.getBlock()) != null) {
            return false;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return canPickSweetBerries(player, state);
        }
        return true;
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
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, 2, radius))) {
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
        double cx = Mth.clamp(eyes.x, pos.getX(), pos.getX() + 1.0);
        double cy = Mth.clamp(eyes.y, pos.getY(), pos.getY() + 1.0);
        double cz = Mth.clamp(eyes.z, pos.getZ(), pos.getZ() + 1.0);
        if (eyes.distanceToSqr(cx, cy, cz) > reach * reach) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 delta = center.subtract(eyes);
        double distSqr = delta.lengthSqr();
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
            if (!canPickSweetBerries(player, state)) {
                return;
            }
            int previous = -1;
            int selected = Slots.selected(player.getInventory());
            // Since 1.20.5 only MAIN_HAND triggers SweetBerryBushBlock harvesting; bone meal at age 2 would fertilize instead.
            if (state.getValue(SweetBerryBushBlock.AGE) == 2 && player.getMainHandItem().is(Items.BONE_MEAL)) {
                int clean = findNonBoneMealHotbar(player);
                if (clean == -1) {
                    return;
                }
                previous = selected;
                Inventories.selectSlot(player, clean);
            }
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            if (previous != -1) {
                Inventories.selectSlot(player, previous);
            }
            Session.swing(player);
            BlockState after = mc.level.getBlockState(pos);
            if (after.getBlock() instanceof SweetBerryBushBlock
                    && after.getValue(SweetBerryBushBlock.AGE) < state.getValue(SweetBerryBushBlock.AGE)) {
                harvested++;
                MultiClicker.get().stats().count(Stat.CROPS);
            }
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

    private static boolean canPickSweetBerries(LocalPlayer player, BlockState state) {
        if (player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) {
            return false;
        }
        return state.getValue(SweetBerryBushBlock.AGE) != 2
                || !player.getMainHandItem().is(Items.BONE_MEAL)
                || findNonBoneMealHotbar(player) != -1;
    }

    private static int findNonBoneMealHotbar(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if ((mod == null || !mod.isSlotBorrowed(slot)) && !player.getInventory().getItem(slot).is(Items.BONE_MEAL)) {
                return slot;
            }
        }
        return -1;
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
        if (++replantTimer > REPLANT_TIMEOUT || !inReachAndVisible(mc, player, player.getEyePosition(), pos, MAX_REACH)) {
            replantPos = null;
            replantFacing = null;
            return false;
        }
        BlockState current = mc.level.getBlockState(pos);
        if (!current.isAir()) {
            if (current.getBlock() == Block.byItem(replantSeed) || isGrowing(current)) {
                replantPos = null;
                replantFacing = null;
            }
            return false; // the harvest is not through yet
        }
        if (!hasItem(player, replantSeed)) {
            return false; // wait within REPLANT_TIMEOUT for dropped seeds to enter inventory
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
        if (planted) {
            replantPos = null;
            replantFacing = null;
        }
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
                if (mainSlot == -1 || !Inventories.requestMainInventoryClick(mc)) {
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
                    int tempHotbar = Inventories.chooseBorrowSlot(player.getInventory());
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
        MultiClicker mod = MultiClicker.get();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int findEmptyHotbar(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if ((mod == null || !mod.isSlotBorrowed(slot)) && player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int findMain(LocalPlayer player, Item item) {
        MultiClicker mod = MultiClicker.get();
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                continue;
            }
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
