package pro.mikey.autoclicker.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.MultiClicker;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /**
     * Camera lock: skip turning the player. The caller clears the accumulated mouse movement right
     * after this method, so the camera does not jump when the lock is released.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void multiclicker$lockCamera(double movementTime, CallbackInfo ci) {
        MultiClicker mod = MultiClicker.get();
        if (mod != null && mod.clicker().isCameraLocked()) {
            ci.cancel();
        }
    }
}
