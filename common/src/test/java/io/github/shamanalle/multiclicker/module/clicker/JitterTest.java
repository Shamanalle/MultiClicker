package io.github.shamanalle.multiclicker.module.clicker;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JitterTest {
    private static final int SAMPLES = 200_000;

    private static double[] sample(Jitter jitter, int max) {
        Random random = new Random(42);
        double[] values = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            values[i] = jitter.sample(max, random);
        }
        return values;
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static double share(double[] values, double from, double to) {
        int count = 0;
        for (double value : values) {
            if (value >= from && value < to) {
                count++;
            }
        }
        return (double) count / values.length;
    }

    @Test
    void noMaximumMeansNoDelay() {
        Random random = new Random(1);
        for (Jitter jitter : Jitter.values()) {
            assertEquals(0, jitter.sample(0, random));
            assertEquals(0, jitter.mean(0));
        }
    }

    @Test
    void uniformIsEvenBetweenZeroAndTheMaximum() {
        double[] values = sample(Jitter.UNIFORM, 10);
        for (double value : values) {
            assertTrue(value >= 0 && value < 10);
        }
        assertEquals(0.25, share(values, 0, 2.5), 0.01);
        assertEquals(0.25, share(values, 7.5, 10), 0.01);
        assertEquals(Jitter.UNIFORM.mean(10), mean(values), 0.05);
    }

    @Test
    void normalGathersAroundTheMiddle() {
        double[] values = sample(Jitter.NORMAL, 10);
        for (double value : values) {
            assertTrue(value >= 0 && value <= 10);
        }
        // Within one standard deviation (max / 5) of the middle: about 68%.
        assertEquals(0.68, share(values, 3, 7), 0.02);
        assertTrue(share(values, 0, 0.5) < 0.02, "the edges are rare");
        assertEquals(Jitter.NORMAL.mean(10), mean(values), 0.05);
    }

    @Test
    void naturalIsMostlyShortWithRareLongPauses() {
        double[] values = sample(Jitter.NATURAL, 10);
        double longPauses = share(values, 10, Double.MAX_VALUE);
        assertEquals(Jitter.PAUSE_CHANCE, longPauses, 0.005);
        for (double value : values) {
            assertTrue(value >= 0 && value <= 30, "a pause is at most three times the maximum");
        }
        assertTrue(share(values, 0, 3.4) > 0.6, "most delays are short");
        assertEquals(Jitter.NATURAL.mean(10), mean(values), 0.1);
    }
}
