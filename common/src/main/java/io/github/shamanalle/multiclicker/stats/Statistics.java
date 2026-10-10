package io.github.shamanalle.multiclicker.stats;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.DamageEvents;
import io.github.shamanalle.multiclicker.compat.Ids;
import io.github.shamanalle.multiclicker.util.ServerStats;

import java.nio.file.Path;

/**
 * Counts what the mod does while it is on. Each event is added to the current session, the
 * current server and the all-time total at once, so a crash loses at most the last minute.
 */
public final class Statistics {
    /** A mob that dies this soon after our last hit counts as our kill. */
    private static final long KILL_CREDIT_MS = 3000;
    /** Unsaved changes are written this often while the mod is on. */
    private static final int SAVE_INTERVAL = 1200;
    /** The new health arrives right after the damage event, in the same batch of packets. */
    private static final int EVENT_WINDOW = 5;
    /** Without damage events the health is awaited for a round trip after the attack. */
    private static final int ATTACK_WINDOW = 20;

    private final StatsStore store;
    private final Counters session = new Counters();
    @Nullable
    private Counters server;
    private String serverAddress = "";
    private final Timeline timeline = new Timeline();
    private final DamageTracker damage = new DamageTracker(this);
    private final FishTracker fish = new FishTracker(this);
    private final LongArrayFIFOQueue recentClicks = new LongArrayFIFOQueue();
    /** Entity id -> time we last hit it. */
    private final Int2LongMap attacked = new Int2LongOpenHashMap();
    private boolean running;
    private long startedAt;
    private long lastTickAt;
    private long secondStart;
    private int secondClicks;
    private boolean fought;
    private int lastXp = -1;
    private boolean wasDead;
    private int saveCountdown;
    private boolean dirty;

    public Statistics(Path file) {
        store = new StatsStore(file);
        store.load();
    }

    // --- Session --------------------------------------------------------------------------------

    /** The mod was turned on: a new session starts. */
    public void start(Minecraft mc) {
        if (running) {
            stop();
        }
        long now = System.currentTimeMillis();
        session.clear();
        timeline.clear();
        damage.clear();
        fish.clear();
        recentClicks.clear();
        attacked.clear();
        String address = ServerStats.address(mc);
        serverAddress = address == null ? "" : address;
        server = address == null ? null : store.server(address);
        startedAt = now;
        lastTickAt = now;
        secondStart = now;
        secondClicks = 0;
        fought = false;
        lastXp = mc.player != null ? mc.player.totalExperience : -1;
        wasDead = false;
        saveCountdown = SAVE_INTERVAL;
        running = true;
        record(Stat.SESSIONS, 1);
    }

    /**
     * The mod was turned off: the session goes into the history and everything is saved.
     *
     * @return the finished session, or {@code null} when none was running
     */
    @Nullable
    public SessionRecord stop() {
        if (!running) {
            return null;
        }
        accrueTime(System.currentTimeMillis());
        running = false;
        damage.clear();
        fish.clear();
        SessionRecord record = new SessionRecord(startedAt, serverAddress, session.copy());
        store.addHistory(record);
        save();
        return record;
    }

    public boolean isRunning() {
        return running;
    }

    public void save() {
        dirty = false;
        store.save();
    }

    /** Forgets all statistics, the current session's included. */
    public void reset() {
        store.clear();
        session.clear();
        timeline.clear();
        if (running) {
            server = serverAddress.isEmpty() ? null : store.server(serverAddress);
        }
        save();
    }

    public void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        while (!recentClicks.isEmpty() && now - recentClicks.firstLong() > 1000) {
            recentClicks.dequeueLong();
        }
        if (!running || mc.level == null || mc.player == null) {
            return;
        }
        accrueTime(now);
        if (now - secondStart > 5000) {
            secondStart = now - 1000; // the game hung: skip the gap instead of filling it with zeros
        }
        while (now - secondStart >= 1000) {
            timeline.second(secondClicks, (float) MultiClicker.get().clicker().targetCps());
            if (fought) {
                record(Stat.COMBAT_TIME, 1000);
            }
            secondClicks = 0;
            fought = false;
            secondStart += 1000;
        }
        tickPlayer(mc.player);
        tickKills(mc, now);
        damage.tick(mc.level);
        fish.tick(mc);
        if (--saveCountdown <= 0) {
            saveCountdown = SAVE_INTERVAL;
            if (dirty) {
                save();
            }
        }
    }

    private void accrueTime(long now) {
        long elapsed = now - lastTickAt;
        lastTickAt = now;
        if (elapsed > 0) {
            record(Stat.ACTIVE_TIME, elapsed);
        }
    }

    private void tickPlayer(LocalPlayer player) {
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            record(Stat.DEATHS, 1);
        }
        wasDead = dead;
        int xp = player.totalExperience;
        if (lastXp >= 0 && xp > lastXp) {
            record(Stat.XP, xp - lastXp);
        }
        lastXp = xp; // spent on enchanting or lost on death: not counted, just followed
    }

    private void tickKills(Minecraft mc, long now) {
        if (attacked.isEmpty()) {
            return;
        }
        var iterator = attacked.int2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Entity entity = mc.level.getEntity(entry.getIntKey());
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                record(Stat.KILLS, 1);
                record(Counters.Group.MOBS, EntityType.getKey(entity.getType()).toString(), 1);
                iterator.remove();
            } else if (entity == null || now - entry.getLongValue() > KILL_CREDIT_MS) {
                iterator.remove();
            }
        }
    }

    private int minute() {
        return (int) ((System.currentTimeMillis() - startedAt) / 60_000);
    }

    private void record(Stat stat, long amount) {
        if (!running || amount == 0) {
            return;
        }
        session.add(stat, amount);
        store.total().add(stat, amount);
        if (server != null) {
            server.add(stat, amount);
        }
        timeline.record(stat, amount, minute());
        dirty = true;
    }

    private void record(Counters.Group group, String id, long amount) {
        if (!running) {
            return;
        }
        session.add(group, id, amount);
        store.total().add(group, id, amount);
        if (server != null) {
            server.add(group, id, amount);
        }
    }

    // --- Events ---------------------------------------------------------------------------------

    /** A click of the clicker, on a target or not. */
    public void recordClick() {
        if (!running) {
            return;
        }
        recentClicks.enqueue(System.currentTimeMillis());
        secondClicks++;
        record(Stat.CLICKS, 1);
    }

    /** The clicker attacked an entity. */
    public void recordAttack(Entity target) {
        if (!running) {
            return;
        }
        record(Stat.ATTACKS, 1);
        attacked.put(target.getId(), System.currentTimeMillis());
        if (!DamageEvents.SUPPORTED && target instanceof LivingEntity living) {
            // Before 1.19.4 the server does not say who hurt whom. A target that is not invulnerable
            // after a recent hit (it shows the hurt animation then) is sure to lose health.
            damage.expect(living, ATTACK_WINDOW, living.hurtTime == 0);
        }
    }

    /** An entity was hurt; {@code causeId} is who caused it, or -1 for nobody. */
    public void onDamageEvent(Minecraft mc, int entityId, int causeId) {
        if (!running || mc.level == null || mc.player == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || entity == mc.player) {
            return;
        }
        if (causeId == mc.player.getId()) {
            attacked.put(entityId, System.currentTimeMillis());
            damage.expect(living, EVENT_WINDOW, true);
        } else {
            damage.settle(living);
        }
    }

    void damageDealt(float amount) {
        record(Stat.DAMAGE, Math.round(amount * 10));
        record(Stat.DAMAGE_MEASURED, 1);
        record(Stat.DAMAGE_HITS, 1);
        fought = true;
    }

    void damageUnseen() {
        record(Stat.DAMAGE_HITS, 1);
    }

    /** Auto fish reeled in a bite. */
    public void onCatch(Minecraft mc, FishingHook hook) {
        if (!running) {
            return;
        }
        record(Stat.CATCHES, 1);
        fish.onCatch(mc, hook);
    }

    void loot(String itemId, int count, boolean enchanted) {
        record(LootKind.classify(itemId, enchanted).stat(), count);
        record(Counters.Group.LOOT, itemId, count);
    }

    public void onBlockBroken(Block block) {
        if (!running) {
            return;
        }
        record(Stat.BLOCKS, 1);
        record(Counters.Group.BLOCKS, Ids.blockId(block), 1);
    }

    /** Counts one of the simple events: crops, food, totems, refills, anti-AFK actions. */
    public void count(Stat stat) {
        record(stat, 1);
    }

    public void count(Stat stat, long amount) {
        record(stat, amount);
    }

    // --- Reading --------------------------------------------------------------------------------

    public Counters session() {
        return session;
    }

    /** The counters of the server being played on, or {@code null} when it is unknown. */
    @Nullable
    public Counters server() {
        return server;
    }

    public String serverAddress() {
        return serverAddress;
    }

    public StatsStore store() {
        return store;
    }

    public Timeline timeline() {
        return timeline;
    }

    public int clicksPerSecond() {
        return recentClicks.size();
    }

    public int clicks() {
        return (int) session.get(Stat.CLICKS);
    }

    public int attacks() {
        return (int) session.get(Stat.ATTACKS);
    }

    public int kills() {
        return (int) session.get(Stat.KILLS);
    }

    /** Time since the mod was turned on; the length of the last session once it is off. */
    public long elapsedMillis() {
        return running ? System.currentTimeMillis() - startedAt : session.get(Stat.ACTIVE_TIME);
    }

    /** When the current or last session started, in epoch milliseconds; 0 before the first. */
    public long startedAt() {
        return startedAt;
    }

    // --- Derived values -------------------------------------------------------------------------

    /**
     * Whether the damage counted is real. Some servers hide the health of other entities; then
     * our hits change nothing and the damage is shown as unknown rather than as zero.
     */
    public static boolean damageKnown(Counters counters) {
        long hits = counters.get(Stat.DAMAGE_HITS);
        return hits < 5 || counters.get(Stat.DAMAGE_MEASURED) * 5 >= hits;
    }

    /** Health points dealt. */
    public static double damage(Counters counters) {
        return counters.get(Stat.DAMAGE) / 10.0;
    }

    /** Average damage per second while fighting, or -1 before any fight. */
    public static double dps(Counters counters) {
        long combat = counters.get(Stat.COMBAT_TIME);
        return combat < 1000 ? -1 : damage(counters) / (combat / 1000.0);
    }

    /** How many per hour of the mod being on, or -1 while there is less than a minute to go by. */
    public static double perHour(Counters counters, Stat stat) {
        long time = counters.get(Stat.ACTIVE_TIME);
        double value = stat == Stat.DAMAGE ? damage(counters) : counters.get(stat);
        return time < 60_000 ? -1 : value * 3_600_000.0 / time;
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
