package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client state whose accessors were added or renamed between Minecraft versions. */
public final class Client {
    private Client() {
    }

    public static int fps(Minecraft mc) {
        return mc.getFps();
    }

    /** Whether the item is enabled by the world's feature flags (experimental items are not). */
    public static boolean itemEnabled(Minecraft mc, ItemStack stack) {
        return stack.isItemEnabled(mc.level.enabledFeatures());
    }
}
