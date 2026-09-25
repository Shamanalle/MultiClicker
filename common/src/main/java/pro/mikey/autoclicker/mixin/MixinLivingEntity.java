package pro.mikey.autoclicker.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ArmorStand) {
            instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                    .ifPresent(clicker -> {
                        Integer forcedTime = clicker.armorStandResilience.get(self.getId());
                        if (forcedTime != null) {
                            if (forcedTime > 0) {
                                self.hurtTime = forcedTime;
                                self.hurtDuration = 10;
                                clicker.armorStandResilience.put(self.getId(), forcedTime - 1);
                            } else {
                                clicker.armorStandResilience.remove(self.getId());
                                self.hurtTime = 0;
                            }
                        }
                    });
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void monitorHealth(float newHealth, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        float currentHealth = entity.getHealth();
        if (newHealth < currentHealth) {
            float realDamage = currentHealth - newHealth;
            instance.getCombatStats().onEntityDamaged(entity, realDamage);
        }
        if (newHealth <= 0.0f) {
            instance.getCombatStats().registerKillCheck(entity);
        }
    }
}
