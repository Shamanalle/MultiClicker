package pro.mikey.autoclicker.util;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Statistics of the current activation session, shown on the HUD. */
public final class SessionStats {
    private static final long KILL_CREDIT_MS = 3000;

    private final LongArrayFIFOQueue recentClicks = new LongArrayFIFOQueue();
    /** Entity id -> time of our last attack on it. */
    private final Int2LongMap attacked = new Int2LongOpenHashMap();
    private long startedAt;
    private int attacks;
    private int kills;

    public void reset() {
        recentClicks.clear();
        attacked.clear();
        startedAt = System.currentTimeMillis();
        attacks = 0;
        kills = 0;
    }

    public void recordClick() {
        recentClicks.enqueue(System.currentTimeMillis());
    }

    public void recordAttack(Entity target) {
        attacks++;
        attacked.put(target.getId(), System.currentTimeMillis());
    }

    public void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        while (!recentClicks.isEmpty() && now - recentClicks.firstLong() > 1000) {
            recentClicks.dequeueLong();
        }
        if (mc.level == null || attacked.isEmpty()) {
            return;
        }
        var iterator = attacked.int2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Entity entity = mc.level.getEntity(entry.getIntKey());
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                kills++;
                iterator.remove();
            } else if (entity == null || now - entry.getLongValue() > KILL_CREDIT_MS) {
                iterator.remove();
            }
        }
    }

    public int clicksPerSecond() {
        return recentClicks.size();
    }

    public int attacks() {
        return attacks;
    }

    public int kills() {
        return kills;
    }

    public long elapsedMillis() {
        return startedAt == 0 ? 0 : System.currentTimeMillis() - startedAt;
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, secs)
                : String.format("%d:%02d", minutes, secs);
    }
}
