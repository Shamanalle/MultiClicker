package pro.mikey.autoclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.mikey.autoclicker.ClientPing;

/**
 * Перехватывает PlayerInfoUpdate пакет. Когда сервер получил наш ответ
 * на KeepAlive, он немедленно шлёт PlayerInfoUpdatePacket с обновлённой
 * задержкой (action = UPDATE_LATENCY). Мы ловим этот момент и кэшируем
 * значение в ClientPing — без null-проверок и без задержки 15–20с ванили.
 */
@Mixin(ClientPacketListener.class)
public class MixinPingPacketListener {

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;

        // Обрабатываем только если в пакете есть action UPDATE_LATENCY
        if (!packet.actions().contains(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY))
            return;

        // Ищем нашего игрока в списке обновлённых записей
        var uuid = mc.player.getUUID();
        for (var entry : packet.entries()) {
            if (!entry.profileId().equals(uuid))
                continue;

            // Читаем уже применённое значение из PlayerInfo
            var info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                ClientPing.update(info.getLatency());
            }
            break;
        }
    }
}
