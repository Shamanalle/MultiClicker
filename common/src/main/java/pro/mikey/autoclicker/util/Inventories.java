package pro.mikey.autoclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

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

    /** Converts an inventory index (0-8 hotbar, 9-35 main) to the player menu slot id. */
    public static int toMenuSlot(int inventoryIndex) {
        return inventoryIndex < HOTBAR_SIZE ? inventoryIndex + 36 : inventoryIndex;
    }

    public static void swapWithOffhand(Minecraft mc, int inventoryIndex) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                toMenuSlot(inventoryIndex), OFFHAND_BUTTON, ClickType.SWAP, player);
    }

    public static void dropStack(Minecraft mc, int inventoryIndex) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                toMenuSlot(inventoryIndex), 1, ClickType.THROW, player);
    }

    /** Selects a hotbar slot; vanilla sends the slot change to the server on its next tick. */
    public static void selectSlot(LocalPlayer player, int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            player.getInventory().setSelectedSlot(slot);
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
