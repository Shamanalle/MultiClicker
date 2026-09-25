package pro.mikey.autoclicker.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(CallbackInfo ci) {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null || !instance.isActiveState())
            return;

        instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                .ifPresent(clicker -> {
                    if (clicker.isCameraLockActive()) {
                        ci.cancel();
                    }
                });
    }
}
