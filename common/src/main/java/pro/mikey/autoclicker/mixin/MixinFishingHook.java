package pro.mikey.autoclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.core.EventBus;

/**
 * Auto-Fish bite detector (refactored to use EventBus).
 *
 * In 1.21, the client syncs a boolean flag `biting` when a fish is hooked.
 * This mixin detects the false→true transition and emits a FishBiteEvent.
 */
@Mixin(FishingHook.class)
public abstract class MixinFishingHook {

    @Shadow
    public boolean biting;

    private boolean ac_prevBiting = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onFishingHookTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null)
            return;

        AutoClicker ac = AutoClicker.getInstance();
        if (ac == null)
            return;

        FishingHook hook = (FishingHook) (Object) this;
        if (hook.getPlayerOwner() != mc.player)
            return;

        // Detect bite: false → true transition
        if (!ac_prevBiting && biting) {
            EventBus.get().emit(new EventBus.FishBiteEvent());
        }

        ac_prevBiting = biting;
    }
}
