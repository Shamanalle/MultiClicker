package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;

/** Inventory clicks in the player's own inventory menu. */
public final class Containers {
    private Containers() {
    }

    /** Swaps the slot with the hotbar slot / offhand given by {@code button} (40 = offhand). */
    public static void swap(Minecraft mc, int menuSlot, int button) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot, button, ContainerInput.SWAP, player);
    }

    /** Throws the whole stack out of the slot. */
    public static void throwStack(Minecraft mc, int menuSlot) {
        LocalPlayer player = mc.player;
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot, 1, ContainerInput.THROW, player);
    }
}
