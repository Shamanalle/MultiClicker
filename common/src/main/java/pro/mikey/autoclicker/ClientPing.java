package pro.mikey.autoclicker;

/**
 * Кэширует пинг игрока из двух источников:
 *
 * <ol>
 * <li>{@code serverPing} — значение из
 * {@code ClientboundPlayerInfoUpdatePacket}
 * (action UPDATE_LATENCY), обновляется миксином
 * {@code MixinPingPacketListener}.</li>
 * <li>{@code keepAlivePing} — собственное измерение RTT из тайминга KeepAlive
 * ID,
 * обновляется миксином {@code MixinKeepAlive}. Работает на BungeeCord/Waterfall
 * серверах (Hypixel и т.д.).</li>
 * </ol>
 *
 * <p>
 * {@link #getPing()} приоритизирует {@code keepAlivePing} (если доступен),
 * так как он измерен клиентом и не зависит от прокси-сервера.
 * Если KeepAlive измерение недоступно (ванильный сервер), используется
 * {@code serverPing} из {@code PlayerInfo.getLatency()}.
 */
public class ClientPing {

    private static volatile int serverPing = -1;
    private static volatile int keepAlivePing = -1;

    /** Вызывается MixinPingPacketListener при UPDATE_LATENCY. */
    public static void update(int latencyMs) {
        if (latencyMs >= 0) {
            serverPing = latencyMs;
        }
    }

    /**
     * Вызывается MixinKeepAlive при получении KeepAlive с timestamp ID.
     * Значение сглаживается EMA для стабильности отображения.
     */
    public static void updateKeepAlive(int rttMs) {
        if (rttMs < 0)
            return;
        int current = keepAlivePing;
        if (current < 0) {
            keepAlivePing = rttMs;
        } else {
            // EMA: 30% нового значения + 70% старого — сглаживает джиттер
            keepAlivePing = (int) (current * 0.7 + rttMs * 0.3);
        }
    }

    /** Сбрасывается при входе/выходе с сервера. */
    public static void reset() {
        serverPing = -1;
        keepAlivePing = -1;
    }

    /**
     * Возвращает наиболее точное значение пинга в миллисекундах,
     * либо -1 если данных ещё нет.
     *
     * <p>
     * Приоритет: keepAlivePing (клиентское измерение) > serverPing (от сервера).
     * На прокси-серверах serverPing = 1ms, keepAlivePing даёт реальный RTT.
     * На ванильных серверах keepAlivePing недоступен, используется serverPing.
     */
    public static int getPing() {
        int k = keepAlivePing;
        if (k >= 0)
            return k;
        int s = serverPing;
        return s >= 0 ? s : -1;
    }
}
