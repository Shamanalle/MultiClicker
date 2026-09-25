package pro.mikey.autoclicker.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.world.HudModule;

@Mixin(Entity.class)
public class MixinEntity {

    // Cached module references — resolved once, avoids Optional boxing per entity
    // per frame
    private static HudModule cachedHud;
    private static CombatClickerModule cachedClicker;
    private static boolean cacheResolved = false;

    private static void resolveCache() {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null) {
            cacheResolved = false;
            return;
        }
        cachedHud = instance.getModuleManager().<HudModule>get("hud").orElse(null);
        cachedClicker = instance.getModuleManager().<CombatClickerModule>get("combat_clicker").orElse(null);
        cacheResolved = (cachedHud != null && cachedClicker != null);
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!cacheResolved)
            resolveCache();
        if (cachedHud == null || cachedClicker == null)
            return;
        if (cachedHud.espMode.get() == HudModule.EspMode.GLOW
                && (Object) this == cachedClicker.getEspTarget()) {
            cir.setReturnValue(true);
        }
    }
}
