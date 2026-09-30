package io.github.shamanalle.multiclicker.compat;

import net.minecraft.world.entity.player.Inventory;

/** Selected hotbar slot access. */
public final class Slots {
    private Slots() {
    }

    public static int selected(Inventory inventory) {
        return inventory.getSelectedSlot();
    }

    public static void select(Inventory inventory, int slot) {
        inventory.setSelectedSlot(slot);
    }
}
