package io.github.shamanalle.multiclicker.module.visual;

import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;

/** Look and feel of the mod itself. */
public class InterfaceModule extends Module {
    public final EnumSetting<Palette> accent = add(new EnumSetting<>("accent", Palette.BLUE));
    public final BoolSetting toggleSound = add(new BoolSetting("toggle_sound", true));
    public final BoolSetting toggleMessage = add(new BoolSetting("toggle_message", true));
    /** The one-time hint with the key bindings was shown. */
    public final BoolSetting welcomeShown = add(new BoolSetting("welcome_shown", false).<BoolSetting>internal().global());

    public InterfaceModule() {
        super("interface", Category.VISUAL, false, true);
    }

    public int accentColor() {
        return accent.get().argb();
    }
}
