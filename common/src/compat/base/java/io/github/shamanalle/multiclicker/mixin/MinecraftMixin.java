package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.shamanalle.multiclicker.MultiClicker;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /**
     * Vanilla only keeps breaking a block while the mouse is grabbed, which is never the case
     * when the game window is in the background. Let a simulated held attack key mine anyway,
     * so AFK mining works with the window unfocused. A wrap instead of a redirect: MixinExtras 0.5.0 to 0.5.4,
     * which several NeoForge versions ship, fail on a redirect.
     */
    @WrapOperation(method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"))
    private boolean multiclicker$allowBackgroundMining(MouseHandler mouseHandler, Operation<Boolean> original) {
        if (original.call(mouseHandler)) {
            return true;
        }
        MultiClicker mod = MultiClicker.get();
        return mod != null && mod.clicker().isHoldingAttack();
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void multiclicker$glowTarget(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        MultiClicker mod = MultiClicker.get();
        if (mod != null && mod.highlight().shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
