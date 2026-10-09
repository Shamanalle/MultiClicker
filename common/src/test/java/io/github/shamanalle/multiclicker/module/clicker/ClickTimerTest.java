package io.github.shamanalle.multiclicker.module.clicker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickTimerTest {
    /** Runs the timer like a click channel that may always click, and counts the clicks. */
    private static int clicks(double interval, int ticks) {
        ClickTimer timer = new ClickTimer();
        int clicks = 0;
        for (int tick = 0; tick < ticks; tick++) {
            timer.tick();
            if (timer.ready()) {
                clicks++;
                timer.restart(interval);
            }
        }
        return clicks;
    }

    @Test
    void wholeIntervalsClickEveryNTicks() {
        assertEquals(20, clicks(1, 20));
        assertEquals(10, clicks(2, 20));
        assertEquals(4, clicks(5, 20));
    }

    @Test
    void fractionsCarryOverSoTheAverageRateIsKept() {
        // 8 clicks per second is 2.5 ticks per click.
        assertEquals(40, clicks(20.0 / 8, 100));
        // 12 clicks per second is 1.67 ticks per click: 12 clicks every 20 ticks.
        assertEquals(120, clicks(20.0 / 12, 200), 1);
        assertEquals(30, clicks(20.0 / 3, 200), 1);
    }

    @Test
    void aClickThatWaitedLongDoesNotCauseABurst() {
        ClickTimer timer = new ClickTimer();
        timer.restart(2);
        for (int tick = 0; tick < 50; tick++) {
            timer.tick(); // nothing to click on for a while
        }
        assertTrue(timer.ready());
        timer.restart(2);
        timer.tick();
        assertTrue(!timer.ready(), "the next click must wait a full interval again");
    }

    @Test
    void resetMakesTheNextClickDue() {
        ClickTimer timer = new ClickTimer();
        timer.restart(10);
        timer.reset();
        assertTrue(timer.ready());
    }
}
