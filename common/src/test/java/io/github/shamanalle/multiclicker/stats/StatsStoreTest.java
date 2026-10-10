package io.github.shamanalle.multiclicker.stats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsStoreTest {
    @TempDir
    Path dir;

    private static Counters sample() {
        Counters counters = new Counters();
        counters.add(Stat.CLICKS, 1200);
        counters.add(Stat.KILLS, 7);
        counters.add(Stat.DAMAGE, 1405);
        counters.add(Stat.ACTIVE_TIME, 600_000);
        counters.add(Counters.Group.MOBS, "minecraft:zombie", 5);
        counters.add(Counters.Group.MOBS, "minecraft:husk", 2);
        counters.add(Counters.Group.LOOT, "minecraft:cod", 3);
        return counters;
    }

    @Test
    void savesAndLoadsEverything() {
        Path file = dir.resolve("multiclicker").resolve("stats.json");
        StatsStore store = new StatsStore(file);
        store.total().addAll(sample());
        store.server("play.example.net").addAll(sample());
        store.addHistory(new SessionRecord(1_700_000_000_000L, "play.example.net", sample()));
        assertTrue(store.save());

        StatsStore loaded = new StatsStore(file);
        loaded.load();
        assertEquals(1200, loaded.total().get(Stat.CLICKS));
        assertEquals(1405, loaded.total().get(Stat.DAMAGE));
        assertEquals(5, loaded.total().get(Counters.Group.MOBS, "minecraft:zombie"));
        assertEquals(7, loaded.servers().get("play.example.net").get(Stat.KILLS));
        assertEquals(1, loaded.history().size());
        SessionRecord record = loaded.history().get(0);
        assertEquals(1_700_000_000_000L, record.start());
        assertEquals("play.example.net", record.server());
        assertEquals(600_000, record.duration());
        assertEquals(3, record.counters().get(Counters.Group.LOOT, "minecraft:cod"));
    }

    @Test
    void historyKeepsTheNewestSessions() {
        StatsStore store = new StatsStore(dir.resolve("stats.json"));
        for (int i = 0; i < StatsStore.HISTORY_SIZE + 5; i++) {
            store.addHistory(new SessionRecord(i, "", new Counters()));
        }
        assertEquals(StatsStore.HISTORY_SIZE, store.history().size());
        assertEquals(StatsStore.HISTORY_SIZE + 4, store.history().get(0).start());
    }

    @Test
    void groupsArePrunedToTheLargest() {
        Counters counters = new Counters();
        for (int i = 0; i < 20; i++) {
            counters.add(Counters.Group.BLOCKS, "minecraft:block_" + i, i + 1);
        }
        counters.prune(3);
        List<Map.Entry<String, Long>> top = counters.top(Counters.Group.BLOCKS, 10);
        assertEquals(3, top.size());
        assertEquals("minecraft:block_19", top.get(0).getKey());
        assertEquals(20, top.get(0).getValue());
    }

    @Test
    void ignoresMalformedValues() throws IOException {
        Path file = dir.resolve("stats.json");
        Files.writeString(file, "{\"total\":{\"clicks\":\"many\",\"kills\":3,\"mobs\":{\"minecraft:zombie\":[1]}},"
                + "\"servers\":[],\"history\":[1,{\"start\":5}]}");
        StatsStore store = new StatsStore(file);
        store.load();
        assertEquals(0, store.total().get(Stat.CLICKS));
        assertEquals(3, store.total().get(Stat.KILLS));
        assertEquals(0, store.total().size(Counters.Group.MOBS));
        assertTrue(store.servers().isEmpty());
        assertTrue(store.history().isEmpty());
    }

    @Test
    void writesOnlyCountersThatAreSet() {
        JsonObject json = sample().toJson();
        assertFalse(json.has(Stat.DEATHS.key()));
        assertEquals(JsonParser.parseString("{\"minecraft:zombie\":5,\"minecraft:husk\":2}"), json.get("mobs"));
    }

    @Test
    void clearForgetsEverything() {
        StatsStore store = new StatsStore(dir.resolve("stats.json"));
        store.total().addAll(sample());
        store.server("a").addAll(sample());
        store.addHistory(new SessionRecord(1, "a", sample()));
        store.clear();
        assertTrue(store.total().isEmpty());
        assertTrue(store.servers().isEmpty());
        assertTrue(store.history().isEmpty());
    }

    @Test
    void exportsCsv() {
        StatsStore store = new StatsStore(dir.resolve("stats.json"));
        store.total().addAll(sample());
        store.server("a,b").addAll(sample());
        store.addHistory(new SessionRecord(0, "a,b", sample()));
        String[] lines = StatsCsv.write(store, java.time.ZoneOffset.UTC).split("\n");
        assertEquals(4, lines.length);
        assertTrue(lines[0].startsWith("scope,server,start,active_time,sessions,clicks"));
        assertTrue(lines[1].startsWith("total,,,600,0,1200,0,7,140.5,"), lines[1]);
        assertTrue(lines[2].startsWith("server,\"a,b\",,600,"), lines[2]);
        assertTrue(lines[3].startsWith("session,\"a,b\",1970-01-01 00:00:00,600,"), lines[3]);
    }
}
