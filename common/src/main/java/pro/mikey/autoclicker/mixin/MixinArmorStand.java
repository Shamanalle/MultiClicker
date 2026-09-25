package pro.mikey.autoclicker.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStand.class)
public class MixinArmorStand {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEvent(byte id, CallbackInfo ci) {
        if (id == 32) {
            AutoClicker instance = AutoClicker.getInstance();
            if (instance == null)
                return;

            instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                    .ifPresent(clicker -> {
                        if (clicker.isEntityProtectionEnabled()) {
                            ArmorStand self = (ArmorStand) (Object) this;
                            clicker.armorStandResilience.put(self.getId(), 10);
                            self.hurtTime = 10;
                            self.hurtDuration = 10;
                        }
                    });
        }
    }
}
