package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/** Screen, HUD visibility and translation lookups that moved between Minecraft versions. */
public final class Screens {
    private Screens() {
    }

    /** The open screen, or {@code null} while playing. */
    public static Screen current(Minecraft mc) {
        return mc.screen;
    }

    public static void open(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    /** Whether the player hid the HUD (F1). */
    public static boolean hudHidden(Minecraft mc) {
        return mc.options.hideGui;
    }

    /** Whether the active language has this translation key. */
    public static boolean hasTranslation(String key) {
        return I18n.exists(key);
    }
}
