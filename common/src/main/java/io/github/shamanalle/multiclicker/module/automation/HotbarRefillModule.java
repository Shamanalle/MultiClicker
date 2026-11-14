package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Stacks;
import io.github.shamanalle.multiclicker.compat.Tools;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;

import java.util.Arrays;

/**
 * Keeps the hotbar stocked from the rest of the inventory:
 * <ul>
 *   <li>a stack that was used up (the last block placed, the last piece of food eaten, a tool
 *   that broke) is replaced by the same item, so a stack thrown away on purpose is not;</li>
 *   <li>optionally a stack running low is topped up before it runs out;</li>
 *   <li>the tool or weapon in hand is swapped for a spare before it breaks, so a tool with
 *   Mending is kept.</li>
 * </ul>
 */
public class HotbarRefillModule extends Module {
    /** Ticks to wait for the server to confirm an inventory change before the next one. */
    private static final int COOLDOWN = 3;

    public final BoolSetting refillStacks = add(new BoolSetting("refill_stacks", true));
    public final IntSetting refillAt = add(new IntSetting("refill_at", 0, 0, 32, Unit.NONE)
            .zeroMeans("multiclicker.value.when_empty")
            .visibleWhen(refillStacks::get));
    public final BoolSetting replaceTools = add(new BoolSetting("replace_tools", true));
    public final IntSetting toolDurability = add(new IntSetting("tool_durability", 10, 1, 100, Unit.NONE)
            .visibleWhen(replaceTools::get));

    /** The hotbar as it was last tick, to tell a stack that was used up from one that was moved. */
    private final ItemStack[] last = new ItemStack[Inventories.HOTBAR_SIZE];
    private final boolean[] reachedOne = new boolean[Inventories.HOTBAR_SIZE];
    private int cooldown;
    private int refills;

    public HotbarRefillModule() {
        super("hotbar_refill", Category.AUTOMATION, true, false);
        Arrays.fill(last, ItemStack.EMPTY);
    }

    @Override
    public void start(Minecraft mc) {
        refills = 0;
        cooldown = 0;
        Arrays.fill(reachedOne, false);
        if (mc.player != null) {
            remember(mc.player);
        }
    }

    @Override
    public void resume(Minecraft mc) {
        int count = refills;
        start(mc);
        refills = count;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (Screens.current(mc) != null || player.isCreative() || player.isSpectator()) {
            // The player may be sorting the inventory: what changes now is not used up.
            remember(player);
            cooldown = Math.max(0, cooldown - 1);
            return;
        }
        // Keep tracking stacks that shrink down to 1 while waiting out cooldowns or movement.
        trackConsumedProgress(player);
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (mod.isConsuming() || mod.clicker().isSwappingWeapon()) {
            // Another module owns the hands/hotbar; evaluate again once it finishes.
            return;
        }
        PendingRefill pending = findPendingRefill(player);
        if (pending == null) {
            remember(player);
            return;
        }
        if (!Inventories.requestMainInventoryClick(mc)) {
            return;
        }
        if (pending.toOffhand) {
            Inventories.swapWithOffhand(mc, pending.sourceSlot);
        } else if (pending.merge) {
            Inventories.mergeInto(mc, pending.sourceSlot, pending.targetSlot);
        } else {
            Inventories.swapWithHotbar(mc, pending.sourceSlot, pending.targetSlot);
        }
        refills++;
        mod.stats().count(Stat.REFILLS);
        cooldown = COOLDOWN;
        remember(player);
    }

    private void trackConsumedProgress(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                last[slot] = ItemStack.EMPTY;
                reachedOne[slot] = false;
                continue;
            }
            ItemStack now = player.getInventory().getItem(slot);
            ItemStack prev = last[slot];
            if (!now.isEmpty() && Stacks.sameItem(prev, now) && now.getCount() != prev.getCount()) {
                last[slot] = now.copy();
                reachedOne[slot] = now.getCount() <= 1;
            }
        }
    }

    private void remember(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                last[slot] = ItemStack.EMPTY;
                reachedOne[slot] = false;
                continue;
            }
            ItemStack now = player.getInventory().getItem(slot);
            last[slot] = now.copy();
            reachedOne[slot] = !now.isEmpty() && now.getCount() == 1;
        }
    }

    /** Whether this module is active and has a spare tool ready in the main inventory. */
    public boolean willReplaceTool(LocalPlayer player, ItemStack worn, int minDurability) {
        return isRunning() && replaceTools.get()
                && findSpareTool(player.getInventory(), worn, Math.max(toolDurability.get(), minDurability)) != -1;
    }

    private PendingRefill findPendingRefill(LocalPlayer player) {
        PendingRefill worn = replaceTools.get() ? findWornTool(player) : null;
        if (worn != null) {
            return worn;
        }
        return refillStacks.get() ? findRefill(player) : null;
    }

    private PendingRefill findWornTool(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        AutoFishModule autoFish = mod.autoFish();
        int selected = Slots.selected(player.getInventory());
        if (mod == null || !mod.isSlotBorrowed(selected)) {
            ItemStack held = player.getInventory().getItem(selected);
            int heldMin = Tools.kind(held) == Tools.FISHING_ROD && autoFish.isRunning()
                    ? Math.max(toolDurability.get(), autoFish.effectiveProtectRod(player)) : toolDurability.get();
            if (isWorn(held, heldMin)) {
                int spare = findSpareTool(player.getInventory(), held, heldMin);
                if (spare != -1) {
                    return new PendingRefill(spare, selected, false, false);
                }
            }
        }
        if (autoFish.fishesWithOffhand(player)) {
            ItemStack offhand = player.getOffhandItem();
            int offhandMin = Math.max(toolDurability.get(), autoFish.effectiveProtectRod(player));
            if (isWorn(offhand, offhandMin)) {
                int spare = findSpareTool(player.getInventory(), offhand, offhandMin);
                if (spare != -1) {
                    return new PendingRefill(spare, -1, false, true);
                }
            }
        }
        return null;
    }

    private PendingRefill findRefill(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                continue;
            }
            ItemStack before = last[slot];
            ItemStack now = inventory.getItem(slot);
            if (before.isEmpty() || mod.inventoryCleaner().throwsAwayFromHotbar(before)) {
                // The inventory cleaner would throw the refill away again.
                continue;
            }
            if (now.isEmpty()) {
                // The last item was used, or a tool broke. A whole stack that disappeared was moved or thrown.
                boolean usedUp = before.isDamageableItem() || before.getCount() == 1 || reachedOne[slot];
                int minTool = replaceTools.get() ? toolDurability.get() : 1;
                int source = !usedUp ? -1 : before.isDamageableItem()
                        ? findSpareTool(inventory, before, minTool) : findSame(inventory, before);
                if (source != -1) {
                    return new PendingRefill(source, slot, false, false);
                }
            } else if (refillAt.get() > 0 && now.isStackable() && now.getCount() <= refillAt.get()
                    && now.getCount() < before.getCount() && Stacks.sameItem(now, before)) {
                int source = findSame(inventory, now);
                if (source != -1) {
                    if (mod.clicker().safeInventory.get()) {
                        if (inventory.getItem(source).getCount() <= now.getCount()) {
                            continue;
                        }
                        return new PendingRefill(source, slot, false, false);
                    }
                    return new PendingRefill(source, slot, true, false);
                }
            }
        }
        return null;
    }

    /** Largest stack of exactly this item in the main inventory (not the hotbar or borrowed slots). */
    private static int findSame(Inventory inventory, ItemStack wanted) {
        MultiClicker mod = MultiClicker.get();
        int best = -1;
        int bestCount = 0;
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (Stacks.sameItem(stack, wanted) && stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                best = slot;
            }
        }
        return best;
    }

    /**
     * The best replacement for a worn or broken tool in the main inventory: the same item with the
     * most durability left, otherwise a tool of the same kind, the most durable (best material) first.
     */
    private int findSpareTool(Inventory inventory, ItemStack tool, int minDurability) {
        MultiClicker mod = MultiClicker.get();
        int kind = Tools.kind(tool);
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            if (mod != null && mod.isSlotBorrowed(slot)) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || isWorn(stack, minDurability)) {
                continue;
            }
            int score;
            if (stack.is(tool.getItem())) {
                score = 1_000_000 + Inventories.remainingDurability(stack);
            } else if (kind != Tools.NONE && Tools.kind(stack) == kind) {
                score = stack.getMaxDamage();
            } else {
                continue;
            }
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private static boolean isWorn(ItemStack stack, int minDurability) {
        int threshold = effectiveMinDurability(stack, minDurability);
        return threshold > 0 && Inventories.isNearlyBroken(stack, threshold);
    }

    private static int effectiveMinDurability(ItemStack stack, int minDurability) {
        if (!stack.isDamageableItem() || minDurability <= 0) {
            return 0;
        }
        int max = stack.getMaxDamage();
        return max > 1 ? Math.min(minDurability, max - 1) : minDurability;
    }

    private record PendingRefill(int sourceSlot, int targetSlot, boolean merge, boolean toOffhand) {
    }

    @Override
    public String hudInfo() {
        return refills > 0 ? Integer.toString(refills) : null;
    }
}
