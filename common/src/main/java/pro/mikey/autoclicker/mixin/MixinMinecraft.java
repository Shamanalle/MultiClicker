package pro.mikey.autoclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At("HEAD"))
    private void capturePrePickEntity(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                .ifPresent(clicker -> {
                    if (mc.player != null && mc.level != null
                            && mc.hitResult instanceof EntityHitResult ehr
                            && ehr.getEntity() instanceof LivingEntity le
                            && le.isAlive() && le.isAttackable()) {
                        clicker.setPrePickEntity(ehr.getEntity());
                    } else {
                        clicker.setPrePickEntity(null);
                    }
                });
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V"))
    private void beforeHandleKeybinds(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        AutoClicker instance = AutoClicker.getInstance();
        if (instance != null && mc.player != null && mc.level != null) {
            instance.clientTickEvent(mc);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearPrePickEntity(CallbackInfo ci) {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                .ifPresent(clicker -> {
                    clicker.setPrePickEntity(null);
                    clicker.consumeFrameLatchedEntity();
                });
    }
}
