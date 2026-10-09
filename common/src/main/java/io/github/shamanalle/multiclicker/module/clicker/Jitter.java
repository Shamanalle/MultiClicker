package io.github.shamanalle.multiclicker.module.clicker;

import java.util.Random;

/**
 * How the random delay added to every click is spread between 0 and its maximum. The value is in
 * ticks and may be fractional; fractions carry over to the next click.
 */
public enum Jitter {
    /** Every delay from 0 to the maximum is equally likely. */
    UNIFORM,
    /** A bell curve around half of the maximum: most clicks keep a steady rhythm. */
    NORMAL,
    /**
     * Mostly short delays, now and then a longer hesitation, like a person clicking: half of a bell
     * curve below a third of the maximum, plus a rare pause of one to three times the maximum.
     */
    NATURAL;

    /** Chance of the rare long pause of {@link #NATURAL}. */
    static final double PAUSE_CHANCE = 0.04;

    /**
     * A random delay in ticks.
     *
     * @param max the maximum delay in ticks; 0 always gives 0
     */
    public double sample(int max, Random random) {
        if (max <= 0) {
            return 0;
        }
        return switch (this) {
            case UNIFORM -> random.nextDouble() * max;
            case NORMAL -> clamp(max / 2.0 + random.nextGaussian() * max / 5.0, max);
            case NATURAL -> random.nextDouble() < PAUSE_CHANCE
                    ? max * (1 + 2 * random.nextDouble())
                    : clamp(Math.abs(random.nextGaussian()) * max / 3.0, max);
        };
    }

    /** The average delay of {@link #sample} in ticks, e.g. to show the average click rate. */
    public double mean(int max) {
        return switch (this) {
            case UNIFORM, NORMAL -> max / 2.0;
            // Half-normal: sigma * sqrt(2 / pi), plus the rare pause of twice the maximum on average.
            case NATURAL -> max * ((1 - PAUSE_CHANCE) * Math.sqrt(2 / Math.PI) / 3.0 + PAUSE_CHANCE * 2);
        };
    }

    private static double clamp(double value, int max) {
        return Math.max(0, Math.min(max, value));
    }
}
