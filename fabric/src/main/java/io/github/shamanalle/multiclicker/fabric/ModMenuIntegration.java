package io.github.shamanalle.multiclicker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;

/** Opens the MultiClicker menu from Mod Menu's mod list. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
