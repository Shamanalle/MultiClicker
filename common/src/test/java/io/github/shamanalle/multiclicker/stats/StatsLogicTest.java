package io.github.shamanalle.multiclicker.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsLogicTest {
    @Test
    void classifiesVanillaLoot() {
        assertEquals(LootKind.FISH, LootKind.classify("minecraft:cod", false));
        assertEquals(LootKind.FISH, LootKind.classify("minecraft:pufferfish", false));
        assertEquals(LootKind.TREASURE, LootKind.classify("minecraft:name_tag", false));
        assertEquals(LootKind.TREASURE, LootKind.classify("minecraft:bow", true));
        assertEquals(LootKind.TREASURE, LootKind.classify("minecraft:fishing_rod", true));
        assertEquals(LootKind.JUNK, LootKind.classify("minecraft:fishing_rod", false));
        assertEquals(LootKind.JUNK, LootKind.classify("minecraft:leather_boots", false));
        assertEquals(LootKind.OTHER, LootKind.classify("evenmorefish:golden_carp", false));
    }

    @Test
    void timelineKeepsTheLastMinuteOfClicks() {
        Timeline timeline = new Timeline();
        for (int i = 0; i < Timeline.SECONDS + 10; i++) {
            timeline.second(i, 12);
        }
        assertEquals(Timeline.SECONDS, timeline.seconds());
        assertEquals(10, timeline.clicksAt(0));
        assertEquals(Timeline.SECONDS + 9, timeline.clicksAt(Timeline.SECONDS - 1));
        assertEquals(12, timeline.targetAt(0));
    }

    @Test
    void timelineCountsPerMinute() {
        Timeline timeline = new Timeline();
        timeline.record(Stat.KILLS, 2, 0);
        timeline.record(Stat.KILLS, 1, 3);
        timeline.record(Stat.FOOD, 1, 3); // not kept per minute
        assertEquals(4, timeline.minutes());
        assertEquals(2, timeline.valueAt(Stat.KILLS, 0));
        assertEquals(0, timeline.valueAt(Stat.KILLS, 1));
        assertEquals(1, timeline.valueAt(Stat.KILLS, 3));
        timeline.record(Stat.CLICKS, 1, Timeline.MINUTES + 10);
        assertEquals(Timeline.MINUTES, timeline.minutes());
        assertEquals(11, timeline.firstMinute());
        assertEquals(1, timeline.valueAt(Stat.CLICKS, Timeline.MINUTES - 1));
    }

    @Test
    void damageIsUnknownWhenHealthNeverChanges() {
        Counters counters = new Counters();
        counters.add(Stat.DAMAGE_HITS, 4);
        assertTrue(Statistics.damageKnown(counters), "too few hits to tell");
        counters.add(Stat.DAMAGE_HITS, 20);
        assertFalse(Statistics.damageKnown(counters));
        counters.add(Stat.DAMAGE_MEASURED, 10);
        assertTrue(Statistics.damageKnown(counters));
    }

    @Test
    void ratesNeedAMinute() {
        Counters counters = new Counters();
        counters.add(Stat.KILLS, 30);
        counters.add(Stat.ACTIVE_TIME, 59_000);
        assertEquals(-1, Statistics.perHour(counters, Stat.KILLS));
        counters.add(Stat.ACTIVE_TIME, 1_000 + 29 * 60_000);
        assertEquals(60, Statistics.perHour(counters, Stat.KILLS), 1e-9);
        assertEquals("60/h", StatFormat.perHour(counters, Stat.KILLS));
    }

    @Test
    void formatsCompactNumbers() {
        assertEquals("999", StatFormat.compact(999));
        assertEquals("1.2k", StatFormat.compact(1234));
        assertEquals("12k", StatFormat.compact(12_345));
        assertEquals("3.4M", StatFormat.compact(3_400_000));
        assertEquals("2.5", StatFormat.compact(2.5));
    }

    @Test
    void dpsCountsOnlyFightingTime() {
        Counters counters = new Counters();
        counters.add(Stat.DAMAGE, 600); // 60 health points
        counters.add(Stat.COMBAT_TIME, 10_000);
        counters.add(Stat.ACTIVE_TIME, 3_600_000);
        assertEquals(6, Statistics.dps(counters), 1e-9);
    }
}
