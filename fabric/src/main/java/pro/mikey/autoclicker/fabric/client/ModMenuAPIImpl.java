package pro.mikey.autoclicker.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import pro.mikey.autoclicker.OptionsScreen;

/**
 * ModMenu integration — returns the mod's config screen when clicked
 * in the ModMenu mod list.
 */
public class ModMenuAPIImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> new OptionsScreen();
    }
}
