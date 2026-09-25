package pro.mikey.autoclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import pro.mikey.autoclicker.mixin.InventoryAccessor;

/**
 * Shared inventory utility methods used across multiple modules.
 * Eliminates code duplication from the old monolithic AutoClicker.
 */
public final class InventoryUtils {

    private InventoryUtils() {
    }

    /** Check if an ItemStack is food (1.21+ DataComponents.FOOD). */
    public static boolean isFoodItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        try {
            return stack.has(net.minecraft.core.component.DataComponents.FOOD);
        } catch (Exception e) {
            return false;
        }
    }

    /** Check if an item is a sword (path contains "sword"). */
    public static boolean isSword(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath().contains("sword");
    }

    /** Check if an item is a pickaxe (path contains "pickaxe"). */
    public static boolean isPickaxe(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath().contains("pickaxe");
    }

    /** Get the currently selected hotbar slot index. */
    public static int getSelectedSlot(Player player) {
        return ((InventoryAccessor) (Object) player.getInventory()).getSelected();
    }

    /** Set the currently selected hotbar slot index (0-8) and synchronize with server. */
    public static void setSelectedSlot(Player player, int slot) {
        if (player == null || slot < 0 || slot > 8)
            return;
        ((InventoryAccessor) (Object) player.getInventory()).setSelected(slot);
        if (player instanceof LocalPlayer lp && lp.connection != null) {
            lp.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    /** Set the currently selected hotbar slot index (0-8) and synchronize with server. */
    public static void setSelectedSlot(Minecraft mc, int slot) {
        if (mc != null && mc.player != null) {
            setSelectedSlot(mc.player, slot);
        }
    }

    /**
     * Find the first occurrence of an item in inventory slots 0-35.
     * Returns the slot index or -1 if not found.
     */
    public static int findItem(Player player, Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item)
                return i;
        }
        return -1;
    }

    /**
     * Find a Totem of Undying in the player's inventory (slots 0-35).
     * Returns the slot index or -1 if not found.
     */
    public static int findTotem(Player player) {
        return findItem(player, Items.TOTEM_OF_UNDYING);
    }

    /**
     * Swap the item at the given raw inventory slot (0-35) into the offhand
     * using the F-key swap packet (ClickType.SWAP, button=40).
     */
    public static void swapToOffhand(Minecraft mc, int invSlot) {
        if (mc.player == null || mc.gameMode == null || invSlot < 0 || invSlot >= 36)
            return;
        int containerSlot = (invSlot < 9) ? (36 + invSlot) : invSlot;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId,
                containerSlot,
                40, // F-key swap with offhand
                ClickType.SWAP,
                mc.player);
    }
}
