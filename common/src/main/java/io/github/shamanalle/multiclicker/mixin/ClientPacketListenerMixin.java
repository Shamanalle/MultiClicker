package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.util.ServerStats;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    /** The entity event of a totem of undying saving its holder. */
    @Unique
    private static final byte TOTEM_USED = 35;

    /**
     * The server sends the world time once per second. TAIL is only reached on the main thread
     * (the network-thread call bails out early to reschedule), so each packet counts once.
     */
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void multiclicker$trackTickRate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerStats.onTimeUpdate();
    }

    /** Counts the totems that saved the player. TAIL runs on the main thread only. */
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void multiclicker$countTotem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        MultiClicker mod = MultiClicker.get();
        if (mod != null && packet.getEventId() == TOTEM_USED && mc.level != null && mc.player != null
                && packet.getEntity(mc.level) == mc.player) {
            mod.stats().count(Stat.TOTEMS);
        }
    }
}
