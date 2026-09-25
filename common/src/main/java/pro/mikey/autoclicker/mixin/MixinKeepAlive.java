package pro.mikey.autoclicker.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.ClientPing;

/**
 * Измеряет пинг аналогично команде /ping на BungeeCord/Waterfall серверах.
 *
 * <p>
 * Принцип: BungeeCord отправляет KeepAlive с
 * {@code id = System.currentTimeMillis()}.
 * Когда клиент получает этот пакет, {@code System.currentTimeMillis() - id}
 * даёт one-way latency (клиент → прокси). Умножаем на 2 для приблизительного
 * RTT.
 *
 * <p>
 * На ванильных серверах ID = {@code Util.getMillis()} (наносекундный таймер),
 * который далёк от {@code System.currentTimeMillis()} — такие пакеты
 * фильтруются
 * проверкой {@code halfRtt >= 0 && halfRtt < 30_000}, и пинг берётся из
 * {@code PlayerInfo.getLatency()} как раньше.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinKeepAlive {

    @Inject(method = "handleKeepAlive", at = @At("HEAD"))
    private void onKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
        long id = packet.getId();
        long now = System.currentTimeMillis();
        long halfRtt = now - id; // ≈ one-way latency если ID — это timestamp

        // BungeeCord/Waterfall: id = System.currentTimeMillis() на прокси.
        // Если разница разумная (0..30 сек), значит id — это timestamp.
        if (halfRtt >= 0 && halfRtt < 30_000) {
            int rtt = Math.max(1, (int) (halfRtt * 2));
            ClientPing.updateKeepAlive(rtt);
        }
    }
}
