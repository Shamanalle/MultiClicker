package pro.mikey.autoclicker.module.visual;

import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;

/** Look and feel of the mod itself. */
public class InterfaceModule extends Module {
    public final EnumSetting<Palette> accent = add(new EnumSetting<>("accent", Palette.BLUE));
    public final BoolSetting toggleSound = add(new BoolSetting("toggle_sound", true));
    public final BoolSetting toggleMessage = add(new BoolSetting("toggle_message", true));
    /** The one-time hint with the key bindings was shown. */
    public final BoolSetting welcomeShown = add(new BoolSetting("welcome_shown", false).internal());

    public InterfaceModule() {
        super("interface", Category.VISUAL, false, true);
    }

    public int accentColor() {
        return accent.get().argb();
    }
}
