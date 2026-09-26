package pro.mikey.autoclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Estimates the server tick rate from the world-time packets (sent once per 20 server ticks)
 * and reads the player's latency from the tab list.
 */
public final class ServerStats {
    private static final int SAMPLES = 10;
    private static final float[] tickRates = new float[SAMPLES];
    private static int sampleCount;
    private static int nextSample;
    private static long lastUpdateNanos = -1;

    private ServerStats() {
    }

    public static synchronized void reset() {
        sampleCount = 0;
        nextSample = 0;
        lastUpdateNanos = -1;
    }

    public static synchronized void onTimeUpdate() {
        long now = System.nanoTime();
        if (lastUpdateNanos > 0) {
            double seconds = (now - lastUpdateNanos) / 1_000_000_000.0;
            if (seconds > 0.05) {
                float tps = (float) Math.min(20.0, 20.0 / seconds);
                tickRates[nextSample] = tps;
                nextSample = (nextSample + 1) % SAMPLES;
                sampleCount = Math.min(SAMPLES, sampleCount + 1);
            }
        }
        lastUpdateNanos = now;
    }

    /** Average server TPS, or {@code -1} while unknown. */
    public static synchronized float tps() {
        if (sampleCount < 2) {
            return -1;
        }
        float sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += tickRates[i];
        }
        return sum / sampleCount;
    }

    /** Latency in milliseconds as reported by the server, or {@code -1} when unknown. */
    public static int ping(Minecraft mc) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return -1;
        }
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }
}
