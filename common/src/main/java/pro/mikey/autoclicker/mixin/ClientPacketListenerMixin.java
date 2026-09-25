package pro.mikey.autoclicker.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.util.ServerStats;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    /**
     * The server sends the world time once per second. TAIL is only reached on the main thread
     * (the network-thread call bails out early to reschedule), so each packet counts once.
     */
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void multiclicker$trackTickRate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerStats.onTimeUpdate();
    }
}
