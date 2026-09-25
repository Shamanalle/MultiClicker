package pro.mikey.autoclicker;

import net.minecraft.util.Mth;

public class ClientTPS {
    private static final float[] TICK_RATES = new float[20];
    private static int nextIndex = 0;
    private static long timeLastTimeUpdate = -1;
    private static long timeGameJoined = -1;

    public static void onGameJoined() {
        java.util.Arrays.fill(TICK_RATES, 0);
        nextIndex = 0;
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    public static void onTimeUpdate() {
        long now = System.currentTimeMillis();
        long cap = now - timeGameJoined;
        if (cap < 2000) { // skip first 2 seconds
            return;
        }

        if (timeLastTimeUpdate != -1) {
            long timeDiff = now - timeLastTimeUpdate;
            float tickTime = timeDiff / 20.0f;
            if (tickTime == 0)
                tickTime = 50;
            float tps = 1000.0f / tickTime;

            if (tps > 20.0f)
                tps = 20.0f;

            TICK_RATES[nextIndex % TICK_RATES.length] = tps;
            nextIndex++;
        }
        timeLastTimeUpdate = now;
    }

    public static float getTPS() {
        if (System.currentTimeMillis() - timeGameJoined < 3000) {
            return 20.0f;
        }

        float num = 0.0f;
        float sum = 0.0f;
        for (float f : TICK_RATES) {
            if (f > 0) {
                sum += f;
                num++;
            }
        }
        return num > 0 ? sum / num : 20.0f;
    }
}
