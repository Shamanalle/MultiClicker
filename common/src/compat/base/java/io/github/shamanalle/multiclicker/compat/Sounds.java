package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Interface sounds: sound events became registry holders in Minecraft 1.19.3. */
public final class Sounds {
    private Sounds() {
    }

    public static SoundInstance click(float pitch, float volume) {
        return SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume);
    }
}
