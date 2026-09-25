package pro.mikey.autoclicker;

/**
 * Tracks combat statistics: kills, real damage dealt, DPS (sliding window), and
 * KPM.
 *
 * DPS is calculated from REAL damage (actual HP loss on targets we attacked)
 * over a sliding window of the last 5 seconds.
 */
public class CombatStats {
    private int killCount = 0;
    private float totalDamageDealt = 0;
    private long sessionStartTime = 0;
    private boolean sessionActive = false;

    // --- Sliding window for DPS (real damage events) ---
    private static final int DPS_WINDOW_SIZE = 1500;
    private static final long DPS_WINDOW_MS = 15000; // 15-second sliding window
    private final float[] damageEvents = new float[DPS_WINDOW_SIZE];
    private final long[] damageTimestamps = new long[DPS_WINDOW_SIZE];
    private int damageWriteIndex = 0;
    private int damageEventCount = 0;

    // --- Ring buffer for KPM (kill timestamps) ---
    private final long[] killTimestamps = new long[1000];
    private int killTimestampIndex = 0;

    // --- Caching for smooth HUD ---
    private float cachedDPS = 0;
    private float cachedKPM = 0;
    private long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 500; // 0.5s

    // --- Attack attribution ---
    // We remember the last attacked entity and a short grace window
    // so we can attribute setHealth damage to our attacks.
    private net.minecraft.world.entity.LivingEntity lastAttackedEntity;
    private long lastAttackTime;
    private static final long DAMAGE_ATTRIBUTION_WINDOW = 1000; // 1 second

    // --- Sweep Logic ---
    private long lastSweepTime = 0;
    private static final long SWEEP_WINDOW_MS = 1000; // 1 second grace period for lag
    private static final float SWEEP_RANGE = 4.5f; // Sweep reaches roughly 1 block + reach

    public void startSession() {
        if (!sessionActive) {
            sessionStartTime = System.currentTimeMillis();
            sessionActive = true;
        }
    }

    public void reset() {
        killCount = 0;
        totalDamageDealt = 0;
        sessionStartTime = System.currentTimeMillis();
        sessionActive = true;
        killTimestampIndex = 0;
        damageWriteIndex = 0;
        damageEventCount = 0;
        cachedDPS = 0;
        cachedKPM = 0;
        lastCacheUpdate = 0;
        lastAttackedEntity = null;
        lastAttackTime = 0;
        for (int i = 0; i < killTimestamps.length; i++) {
            killTimestamps[i] = 0;
        }
        for (int i = 0; i < DPS_WINDOW_SIZE; i++) {
            damageEvents[i] = 0;
            damageTimestamps[i] = 0;
        }
    }

    /**
     * Called from attack mixin to register which entity we attacked.
     */
    public void registerAttack(net.minecraft.world.entity.LivingEntity target) {
        this.lastAttackedEntity = target;
        this.lastAttackTime = System.currentTimeMillis();

        if (!sessionActive) {
            startSession();
        }
    }

    public void registerSweep() {
        this.lastSweepTime = System.currentTimeMillis();
    }

    /**
     * Called from setHealth mixin when an entity loses HP.
     * Only records damage if we recently attacked this entity.
     */
    public void onEntityDamaged(net.minecraft.world.entity.LivingEntity entity, float realDamage) {
        if (realDamage <= 0)
            return;

        boolean isDirectHit = entity == lastAttackedEntity &&
                System.currentTimeMillis() - lastAttackTime < DAMAGE_ATTRIBUTION_WINDOW;

        boolean isSweepHit = false;
        if (!isDirectHit && System.currentTimeMillis() - this.lastSweepTime < SWEEP_WINDOW_MS) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && entity.distanceTo(player) < SWEEP_RANGE) {
                isSweepHit = true;
            }
        }

        // Only count damage if it's a direct hit or a valid sweep hit
        if (isDirectHit || isSweepHit) {
            recordRealDamage(realDamage);
        }
    }

    /**
     * Records real damage into the sliding window.
     */
    private void recordRealDamage(float damage) {
        if (!sessionActive) {
            startSession();
        }

        totalDamageDealt += damage;

        int idx = damageWriteIndex % DPS_WINDOW_SIZE;
        damageEvents[idx] = damage;
        damageTimestamps[idx] = System.currentTimeMillis();
        damageWriteIndex++;
        if (damageEventCount < DPS_WINDOW_SIZE) {
            damageEventCount++;
        }
    }

    /**
     * Legacy method kept for API compatibility. No longer records estimated damage.
     */
    public void recordDamage(float damage) {
        // Intentionally empty — real damage is tracked via onEntityDamaged
    }

    /**
     * Legacy method kept for API compatibility.
     */
    public void recordAttack(net.minecraft.world.entity.LivingEntity entity) {
        registerAttack(entity);
    }

    public void recordKill() {
        killCount++;
        long now = System.currentTimeMillis();
        killTimestamps[killTimestampIndex % killTimestamps.length] = now;
        killTimestampIndex++;

        if (!sessionActive) {
            startSession();
        }
    }

    public void registerKillCheck(net.minecraft.world.entity.LivingEntity diedEntity) {
        boolean isDirectKill = diedEntity == this.lastAttackedEntity &&
                System.currentTimeMillis() - this.lastAttackTime < 5000;

        boolean isSweepKill = false;
        if (!isDirectKill && System.currentTimeMillis() - this.lastSweepTime < SWEEP_WINDOW_MS) {
            // Check distance to player
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && diedEntity.distanceTo(player) < SWEEP_RANGE) {
                isSweepKill = true;
            }
        }

        if (isDirectKill || isSweepKill) {
            this.recordKill();
            // Fire event so CombatFeedbackModule can play kill sound
            pro.mikey.autoclicker.core.EventBus.get().emit(
                    new pro.mikey.autoclicker.core.EventBus.EntityDeathEvent(diedEntity));
            if (isDirectKill) {
                this.lastAttackedEntity = null;
            }
        }
    }

    public int getKillCount() {
        return killCount;
    }

    public float getTotalDamage() {
        return totalDamageDealt;
    }

    /**
     * DPS based on a 5-second sliding window of real damage dealt.
     */
    public float getDPS() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate >= CACHE_UPDATE_INTERVAL) {
            updateCache(now);
        }
        return cachedDPS;
    }

    /**
     * KPM (Kills Per Minute).
     */
    public float getKPM() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate >= CACHE_UPDATE_INTERVAL) {
            updateCache(now);
        }
        return cachedKPM;
    }

    private void updateCache(long now) {
        lastCacheUpdate = now;

        // === DPS: sum of damage in the last DPS_WINDOW_MS ===
        float windowDamage = 0;
        long windowStart = now - DPS_WINDOW_MS;

        int count = Math.min(damageEventCount, DPS_WINDOW_SIZE);
        for (int i = 0; i < count; i++) {
            if (damageTimestamps[i] >= windowStart) {
                windowDamage += damageEvents[i];
            }
        }

        if (windowDamage > 0) {
            // How long we've been in the session or the window size — whichever is smaller
            long elapsed = now - sessionStartTime;
            float windowSeconds = Math.min(elapsed, DPS_WINDOW_MS) / 1000f;
            if (windowSeconds > 0) {
                cachedDPS = windowDamage / windowSeconds;
            } else {
                cachedDPS = windowDamage;
            }
        } else {
            cachedDPS = 0;
        }

        // === KPM: kills in last 60 seconds ===
        if (killCount == 0) {
            cachedKPM = 0;
        } else {
            long oneMinuteAgo = now - 60000;
            int recentKills = 0;
            for (int i = 0; i < Math.min(killCount, killTimestamps.length); i++) {
                if (killTimestamps[i] >= oneMinuteAgo) {
                    recentKills++;
                }
            }

            long elapsedMs = now - sessionStartTime;
            if (elapsedMs < 60000 && elapsedMs > 0) {
                cachedKPM = (killCount * 60000f) / elapsedMs;
            } else {
                cachedKPM = recentKills;
            }
        }
    }

    public long getSessionDurationSeconds() {
        if (!sessionActive) {
            return 0;
        }
        return (System.currentTimeMillis() - sessionStartTime) / 1000;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }
}
