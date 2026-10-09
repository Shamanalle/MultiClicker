package io.github.shamanalle.multiclicker.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.util.Hotkeys;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    /** Vanilla reports every key and mouse button pressed while playing here, bound or not. */
    @Inject(method = "click", at = @At("HEAD"))
    private static void multiclicker$onPress(InputConstants.Key key, CallbackInfo ci) {
        Hotkeys.onPress(key);
    }

    @Inject(method = "set", at = @At("HEAD"))
    private static void multiclicker$onSet(InputConstants.Key key, boolean down, CallbackInfo ci) {
        if (!down) {
            Hotkeys.onRelease(key);
        }
    }
}
