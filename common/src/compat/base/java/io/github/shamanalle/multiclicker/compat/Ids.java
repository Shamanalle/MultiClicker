package io.github.shamanalle.multiclicker.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Resource identifiers: the class was renamed in newer versions, so code outside this package never names it. */
public final class Ids {
    private Ids() {
    }

    /** An identifier of this mod, e.g. for the HUD element. */
    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath("multiclicker", path);
    }

    public static boolean itemExists(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location != null && BuiltInRegistries.ITEM.containsKey(location);
    }

    public static boolean blockExists(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location != null && BuiltInRegistries.BLOCK.containsKey(location);
    }

    /** The item with this id, or air when unknown. */
    public static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(location);
    }

    /** The block with this id, or air when unknown. */
    public static Block block(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getValue(location);
    }
}
