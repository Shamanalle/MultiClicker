package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.MultiClicker;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    @Final
    private SmoothDouble smoothTurnX;

    @Shadow
    @Final
    private SmoothDouble smoothTurnY;

    @Shadow
    private boolean isLeftPressed;

    @Shadow
    private boolean isMiddlePressed;

    @Shadow
    private boolean isRightPressed;

    /**
     * Camera lock: skip turning the player and clear accumulated mouse movement (1.19 - 1.20.4
     * cleared those fields inside turnPlayer itself, so cancelling at HEAD would store up the motion
     * and snap the view on unlock).
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void multiclicker$lockCamera(CallbackInfo ci) {
        MultiClicker mod = MultiClicker.get();
        if (mod != null && mod.clicker().isCameraLocked()) {
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            smoothTurnX.reset();
            smoothTurnY.reset();
            ci.cancel();
        }
    }

    /**
     * Vanilla updates isLeft/Middle/RightPressed only while no screen or overlay is open and never
     * clears them on grab/release, so a mouse button released inside a menu would otherwise stay true.
     */
    @Inject(method = {"grabMouse", "releaseMouse"}, at = @At("HEAD"))
    private void multiclicker$clearButtons(CallbackInfo ci) {
        isLeftPressed = false;
        isMiddlePressed = false;
        isRightPressed = false;
    }
}
