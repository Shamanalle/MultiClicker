package pro.mikey.autoclicker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import pro.mikey.autoclicker.gui.ConfigScreen;

/** Opens the MultiClicker menu from Mod Menu's mod list. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
