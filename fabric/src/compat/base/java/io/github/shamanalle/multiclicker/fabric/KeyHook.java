package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Registers a key binding. */
final class KeyHook {
    private KeyHook() {
    }

    static void register(KeyMapping key) {
        KeyBindingHelper.registerKeyBinding(key);
    }
}
