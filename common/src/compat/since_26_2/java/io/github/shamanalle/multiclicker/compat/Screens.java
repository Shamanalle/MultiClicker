package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;

/** Screen, HUD visibility and translation lookups that moved between Minecraft versions. */
public final class Screens {
    private Screens() {
    }

    /** The open screen, or {@code null} while playing. */
    public static Screen current(Minecraft mc) {
        return mc.gui.screen();
    }

    public static void open(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    /** Whether the player hid the HUD (F1). */
    public static boolean hudHidden(Minecraft mc) {
        return mc.gui.hud.isHidden();
    }

    /** Whether the active language has this translation key. */
    public static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
