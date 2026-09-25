package pro.mikey.autoclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "pick", at = @At("TAIL"))
    private void afterPick(CallbackInfo ci) {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        if (mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le
                && le.isAlive() && le.isAttackable()) {
            instance.getModuleManager().<CombatClickerModule>get("combat_clicker")
                    .ifPresent(clicker -> clicker.setFrameLatchedEntity(le));
        }
    }
}
