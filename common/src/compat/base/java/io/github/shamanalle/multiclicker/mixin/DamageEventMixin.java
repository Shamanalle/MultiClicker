package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.MultiClicker;

@Mixin(ClientPacketListener.class)
public abstract class DamageEventMixin {
    /**
     * Tells the statistics who hurt an entity. The packet comes before the entity's new health, so
     * the health seen here is still the one from before the hit. TAIL runs on the main thread only.
     */
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void multiclicker$countDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        MultiClicker mod = MultiClicker.get();
        if (mod != null) {
            mod.stats().onDamageEvent(Minecraft.getInstance(), packet.entityId(), packet.sourceCauseId());
        }
    }
}
