package pro.mikey.autoclicker.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.ClientTPS;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void onHandleSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ClientTPS.onTimeUpdate();
    }
}
