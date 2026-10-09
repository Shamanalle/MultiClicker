package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;

/** Inventory clicks in the player's own inventory menu. */
public final class Containers {
    private Containers() {
    }

    /** Swaps the slot with the hotbar slot / offhand given by {@code button} (40 = offhand). */
    public static void swap(Minecraft mc, int menuSlot, int button) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot, button, ClickType.SWAP, player);
    }

    /** Throws the whole stack out of the slot. */
    public static void throwStack(Minecraft mc, int menuSlot) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot, 1, ClickType.THROW, player);
    }

    /** A left click on the slot: picks the stack up, or puts down / merges the stack on the cursor. */
    public static void pickup(Minecraft mc, int menuSlot) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot, 0, ClickType.PICKUP, player);
    }
}
