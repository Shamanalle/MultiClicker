package io.github.shamanalle.multiclicker.boot;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu entrypoint: hands out the settings screen of the variant for the running Minecraft version. */
public final class ModMenuBoot implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        Object entry = Variants.create("modmenu");
        return entry != null ? ((ModMenuApi) entry).getModConfigScreenFactory() : ModMenuApi.super.getModConfigScreenFactory();
    }
}
