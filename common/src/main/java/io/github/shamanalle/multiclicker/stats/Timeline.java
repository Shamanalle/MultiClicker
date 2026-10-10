package io.github.shamanalle.multiclicker.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recent activity of the session for the graphs: clicks per second over the last minute against
 * the configured rate, and a few counters per minute of the session.
 */
public final class Timeline {
    public static final int SECONDS = 60;
    /** Minutes kept; older ones are dropped. */
    public static final int MINUTES = 240;
    /** The counters kept per minute. */
    public static final List<Stat> METRICS = List.of(Stat.CLICKS, Stat.ATTACKS, Stat.KILLS, Stat.DAMAGE,
            Stat.CATCHES, Stat.CROPS, Stat.BLOCKS, Stat.XP);

    private final int[] clicks = new int[SECONDS];
    /** Configured clicks per second, negative where nothing was set to click at a fixed rate. */
    private final float[] targets = new float[SECONDS];
    private int nextSecond;
    private int seconds;
    private final List<long[]> minutes = new ArrayList<>();
    /** Minutes dropped from the front, so the minute numbers stay the same. */
    private int droppedMinutes;

    public void clear() {
        Arrays.fill(clicks, 0);
        Arrays.fill(targets, -1);
        nextSecond = 0;
        seconds = 0;
        minutes.clear();
        droppedMinutes = 0;
    }

    public Timeline() {
        clear();
    }

    /** A second of the session ended with {@code clickCount} clicks while the clicker aimed for {@code target}. */
    public void second(int clickCount, float target) {
        clicks[nextSecond] = clickCount;
        targets[nextSecond] = target;
        nextSecond = (nextSecond + 1) % SECONDS;
        seconds = Math.min(SECONDS, seconds + 1);
    }

    /** Seconds recorded so far, at most {@link #SECONDS}. */
    public int seconds() {
        return seconds;
    }

    /** Clicks in the {@code index}-th recorded second, oldest first. */
    public int clicksAt(int index) {
        return clicks[slot(index)];
    }

    public float targetAt(int index) {
        return targets[slot(index)];
    }

    private int slot(int index) {
        return Math.floorMod(nextSecond - seconds + index, SECONDS);
    }

    /** Adds to the counter of the given minute of the session (0 = the first). */
    public void record(Stat stat, long amount, int minute) {
        int metric = METRICS.indexOf(stat);
        if (metric < 0 || minute < droppedMinutes) {
            return;
        }
        while (droppedMinutes + minutes.size() <= minute) {
            minutes.add(new long[METRICS.size()]);
        }
        minutes.get(minute - droppedMinutes)[metric] += amount;
        while (minutes.size() > MINUTES) {
            minutes.remove(0);
            droppedMinutes++;
        }
    }

    /** Minutes kept, the current one included. */
    public int minutes() {
        return minutes.size();
    }

    /** The number of the first minute kept. */
    public int firstMinute() {
        return droppedMinutes;
    }

    /** The counter in the {@code index}-th kept minute. */
    public long valueAt(Stat stat, int index) {
        int metric = METRICS.indexOf(stat);
        return metric < 0 || index < 0 || index >= minutes.size() ? 0 : minutes.get(index)[metric];
    }
}
