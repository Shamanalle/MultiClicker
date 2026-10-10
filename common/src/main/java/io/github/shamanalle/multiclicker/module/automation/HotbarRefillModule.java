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
        if (mc.player != null) {
            remember(mc.player);
        }
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (Screens.current(mc) != null || player.isCreative() || player.isSpectator()) {
            // The player may be sorting the inventory: what changes now is not used up.
            remember(player);
            return;
        }
        if (!Inventories.canClickInventory(mc) || mod.autoEat().isBusy() || mod.clicker().isSwappingWeapon()) {
            // Another module has the hotbar; look again once it is done, against the hotbar from before.
            return;
        }
        if (replaceTools.get() && replaceWornTool(mc, player) || refillStacks.get() && refill(mc, player)) {
            refills++;
            mod.stats().count(Stat.REFILLS);
            cooldown = COOLDOWN;
        }
        remember(player);
    }

    private void remember(LocalPlayer player) {
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            last[slot] = player.getInventory().getItem(slot).copy();
        }
    }

    private boolean replaceWornTool(Minecraft mc, LocalPlayer player) {
        int selected = Slots.selected(player.getInventory());
        ItemStack held = player.getInventory().getItem(selected);
        if (!Inventories.isNearlyBroken(held, toolDurability.get())) {
            return false;
        }
        int spare = findSpareTool(player.getInventory(), held);
        if (spare == -1) {
            return false;
        }
        Inventories.swapWithHotbar(mc, spare, selected);
        return true;
    }

    private boolean refill(Minecraft mc, LocalPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack before = last[slot];
            ItemStack now = inventory.getItem(slot);
            if (before.isEmpty()) {
                continue;
            }
            if (now.isEmpty()) {
                // The last item was used, or a tool broke. A whole stack that disappeared was moved or thrown.
                boolean usedUp = before.getCount() == 1 || before.isDamageableItem();
                int source = !usedUp ? -1 : before.isDamageableItem() ? findSpareTool(inventory, before) : findSame(inventory, before);
                if (source != -1) {
                    Inventories.swapWithHotbar(mc, source, slot);
                    return true;
                }
            } else if (refillAt.get() > 0 && now.isStackable() && now.getCount() <= refillAt.get()
                    && now.getCount() < before.getCount() && Stacks.sameItem(now, before)) {
                int source = findSame(inventory, now);
                if (source != -1) {
                    Inventories.mergeInto(mc, source, slot);
                    return true;
                }
            }
        }
        return false;
    }

    /** A stack of exactly this item in the main inventory (not the hotbar). */
    private static int findSame(Inventory inventory, ItemStack wanted) {
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            if (Stacks.sameItem(inventory.getItem(slot), wanted)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * The best replacement for a worn or broken tool in the main inventory: the same item with the
     * most durability left, otherwise a tool of the same kind, the most durable (best material) first.
     */
    private int findSpareTool(Inventory inventory, ItemStack tool) {
        int kind = Tools.kind(tool);
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int slot = Inventories.HOTBAR_SIZE; slot < Inventories.MAIN_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || Inventories.isNearlyBroken(stack, toolDurability.get())) {
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

    @Override
    public String hudInfo() {
        return refills > 0 ? Integer.toString(refills) : null;
    }
}
