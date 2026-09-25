package pro.mikey.autoclicker.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Accessor("destroyProgress")
    public abstract void setDestroyProgress(float progress);

    @Accessor("destroyProgress")
    public abstract float getDestroyProgress();

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void beforeAttack(Player player, Entity target, CallbackInfo ci) {
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        CombatClickerModule clicker = instance.getModuleManager()
                .<CombatClickerModule>get("combat_clicker").orElse(null);
        if (clicker == null)
            return;

        // Entity Protection: cancel if awaiting server confirmation
        if (clicker.isEntityProtectionEnabled() && clicker.isEntityProtBusy()) {
            ci.cancel();
            return;
        }

        // Combat Stats tracking
        if (target instanceof LivingEntity livingTarget) {
            instance.getCombatStats().registerAttack(livingTarget);
        }

        if (CombatClickerModule.isSword(player.getMainHandItem().getItem())) {
            float attackStrength = player.getAttackStrengthScale(0.5f);
            if (attackStrength > 0.9f && !player.isSprinting() &&
                    (player.onGround() || player.fallDistance <= 0.0F)) {
                instance.getCombatStats().registerSweep();
            }
        }

        // Entity Protection: record the outgoing attack
        if (clicker.isEntityProtectionEnabled()
                && target instanceof LivingEntity
                && !target.isInvulnerable()) {
            clicker.recordEntityProtAttack(target);
        }
    }
}
