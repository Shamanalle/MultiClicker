package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client state whose accessors were added or renamed between Minecraft versions. */
public final class Client {
    private Client() {
    }

    /** Read from the debug text ("60 fps T: ..."): there is no getter before Minecraft 1.19.3. */
    public static int fps(Minecraft mc) {
        String text = mc.fpsString;
        int end = text.indexOf(' ');
        try {
            return Integer.parseInt(end < 0 ? text : text.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Feature flags came with Minecraft 1.19.3: every item is enabled before. */
    public static boolean itemEnabled(Minecraft mc, ItemStack stack) {
        return true;
    }
}
