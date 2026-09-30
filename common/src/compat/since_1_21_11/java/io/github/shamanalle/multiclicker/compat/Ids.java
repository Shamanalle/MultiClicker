package io.github.shamanalle.multiclicker.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Resource identifiers: the class was renamed in newer versions, so code outside this package never names it. */
public final class Ids {
    private Ids() {
    }

    /** An identifier of this mod, e.g. for the HUD element. */
    public static Identifier mod(String path) {
        return Identifier.fromNamespaceAndPath("multiclicker", path);
    }

    public static boolean itemExists(String id) {
        Identifier location = Identifier.tryParse(id);
        return location != null && BuiltInRegistries.ITEM.containsKey(location);
    }

    public static boolean blockExists(String id) {
        Identifier location = Identifier.tryParse(id);
        return location != null && BuiltInRegistries.BLOCK.containsKey(location);
    }

    /** The item with this id, or air when unknown. */
    public static Item item(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(location);
    }

    /** The block with this id, or air when unknown. */
    public static Block block(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getValue(location);
    }
}
