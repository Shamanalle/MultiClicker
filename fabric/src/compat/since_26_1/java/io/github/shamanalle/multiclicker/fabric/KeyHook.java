package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** Registers a key binding. */
final class KeyHook {
    private KeyHook() {
    }

    static void register(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }
}
