package io.github.shamanalle.multiclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Containers;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Tools;

/** Inventory helpers that go through vanilla container clicks, so the server stays in sync. */
public final class Inventories {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = Inventory.INVENTORY_SIZE;
    private static final int OFFHAND_BUTTON = Inventory.SLOT_OFFHAND;

    private Inventories() {
    }

    /**
     * Inventory clicks are only safe while no other container is open and the cursor is empty,
     * otherwise the click would target the wrong menu or drop the carried stack.
     */
    public static boolean canClickInventory(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && mc.gameMode != null
                && player.containerMenu == player.inventoryMenu
                && player.inventoryMenu.getCarried().isEmpty();
    }

    /**
     * Like {@link #canClickInventory(Minecraft)}, and when server-safe inventory mode is enabled,
     * also waits until the player is not pressing movement keys so strict anti-cheats do not flag
     * backpack interactions while moving.
     */
    public static boolean canClickMainInventory(Minecraft mc) {
        if (!canClickInventory(mc)) {
            return false;
        }
        MultiClicker mod = MultiClicker.get();
        return mod == null || !mod.clicker().safeInventory.get() || !Input.isMovementActive(mc);
    }

    /**
     * Returns {@code true} when a module may safely send main-inventory clicks this tick; when
     * {@code safeInventory} is only blocked by {@code AutoWalk}, pauses {@code AutoWalk} so the
     * click can run on the next stationary tick.
     */
    public static boolean requestMainInventoryClick(Minecraft mc) {
        if (!canClickInventory(mc)) {
            return false;
        }
        if (canClickMainInventory(mc)) {
            return true;
        }
        MultiClicker mod = MultiClicker.get();
        if (mod != null) {
            mod.autoWalk().pauseForInventory(mc);
        }
        return false;
    }

    /** Picks a hotbar slot that can temporarily hold an item swapped from the main inventory. */
    public static int chooseBorrowSlot(Inventory inventory) {
        int selected = Slots.selected(inventory);
        MultiClicker mod = MultiClicker.get();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (i == selected || (mod != null && mod.isSlotBorrowed(i))) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            if (!Tools.isWeapon(stack) && !stack.is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (i != selected && (mod == null || !mod.isSlotBorrowed(i))) {
                return i;
            }
        }
        return (selected + 1) % HOTBAR_SIZE;
    }

    /** Converts an inventory index (0-8 hotbar, 9-35 main) to the player menu slot id. */
    public static int toMenuSlot(int inventoryIndex) {
        return inventoryIndex < HOTBAR_SIZE ? inventoryIndex + 36 : inventoryIndex;
    }

    public static void swapWithOffhand(Minecraft mc, int inventoryIndex) {
        Containers.swap(mc, toMenuSlot(inventoryIndex), OFFHAND_BUTTON);
    }

    /** Swaps a main inventory stack with a hotbar slot (either may be empty). */
    public static void swapWithHotbar(Minecraft mc, int inventoryIndex, int hotbarSlot) {
        Containers.swap(mc, toMenuSlot(inventoryIndex), hotbarSlot);
    }

    /**
     * Moves as much of one stack as fits onto another stack of the same item, with cursor clicks
     * like a player would; the rest goes back where it came from.
     */
    public static void mergeInto(Minecraft mc, int fromIndex, int toIndex) {
        Containers.pickup(mc, toMenuSlot(fromIndex));
        Containers.pickup(mc, toMenuSlot(toIndex));
        if (!mc.player.inventoryMenu.getCarried().isEmpty()) {
            Containers.pickup(mc, toMenuSlot(fromIndex));
        }
    }

    public static void dropStack(Minecraft mc, int inventoryIndex) {
        Containers.throwStack(mc, toMenuSlot(inventoryIndex));
    }

    /** Selects a hotbar slot; vanilla sends the slot change to the server on its next tick. */
    public static void selectSlot(LocalPlayer player, int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            Slots.select(player.getInventory(), slot);
        }
    }

    public static int remainingDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /** Whether the stack is a damageable item that is about to break. */
    public static boolean isNearlyBroken(ItemStack stack, int minDurability) {
        return minDurability > 0 && stack.isDamageableItem() && remainingDurability(stack) <= minDurability;
    }
}
