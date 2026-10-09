package io.github.shamanalle.multiclicker.module.clicker;

/**
 * Counts down to the next click in fractional ticks. A click can only happen on a tick, but the
 * remainder carries over: an interval of 1.5 ticks clicks after 2, 1, 2, 1... ticks, so a rate of
 * 12 clicks per second is kept on average although a second has 20 ticks.
 */
public final class ClickTimer {
    private double remaining;

    /** Advances one tick. */
    public void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

    /** Whether the next click is due. */
    public boolean ready() {
        return remaining <= 0;
    }

    /**
     * Starts the wait for the next click after one happened. The fraction by which this click
     * was late is subtracted, so the average rate stays exact.
     */
    public void restart(double ticks) {
        remaining = Math.max(remaining, -1) + ticks;
    }

    /** Ticks left until the next click is due (0 or less when it is due). */
    public double remaining() {
        return remaining;
    }

    public void reset() {
        remaining = 0;
    }
}
