package io.github.shamanalle.multiclicker.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A set of counters: one number per {@link Stat} plus counts per mob, caught item and mined block. */
public final class Counters {
    /** Counts by id, e.g. kills per mob type. */
    public enum Group {
        MOBS, LOOT, BLOCKS;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final long[] values = new long[Stat.values().length];
    private final Map<Group, Map<String, Long>> groups = new EnumMap<>(Group.class);

    public Counters() {
        for (Group group : Group.values()) {
            groups.put(group, new HashMap<>());
        }
    }

    public long get(Stat stat) {
        return values[stat.ordinal()];
    }

    public void add(Stat stat, long amount) {
        values[stat.ordinal()] += amount;
    }

    public void add(Group group, String id, long amount) {
        groups.get(group).merge(id, amount, Long::sum);
    }

    public long get(Group group, String id) {
        return groups.get(group).getOrDefault(id, 0L);
    }

    /** All counts of a group, largest first (ties by id, so the order is stable). */
    public List<Map.Entry<String, Long>> top(Group group, int limit) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(groups.get(group).entrySet());
        entries.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        return entries.size() > limit ? List.copyOf(entries.subList(0, limit)) : List.copyOf(entries);
    }

    public int size(Group group) {
        return groups.get(group).size();
    }

    public void addAll(Counters other) {
        for (int i = 0; i < values.length; i++) {
            values[i] += other.values[i];
        }
        for (Group group : Group.values()) {
            other.groups.get(group).forEach((id, amount) -> add(group, id, amount));
        }
    }

    /** Keeps the {@code limit} largest counts of each group, so the file cannot grow without bound. */
    public void prune(int limit) {
        for (Group group : Group.values()) {
            Map<String, Long> map = groups.get(group);
            if (map.size() > limit) {
                List<Map.Entry<String, Long>> kept = top(group, limit);
                map.clear();
                kept.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            }
        }
    }

    public void clear() {
        Arrays.fill(values, 0);
        groups.values().forEach(Map::clear);
    }

    public boolean isEmpty() {
        for (long value : values) {
            if (value != 0) {
                return false;
            }
        }
        return groups.values().stream().allMatch(Map::isEmpty);
    }

    public Counters copy() {
        Counters copy = new Counters();
        copy.addAll(this);
        return copy;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (Stat stat : Stat.values()) {
            if (get(stat) != 0) {
                json.addProperty(stat.key(), get(stat));
            }
        }
        for (Group group : Group.values()) {
            if (!groups.get(group).isEmpty()) {
                JsonObject map = new JsonObject();
                top(group, Integer.MAX_VALUE).forEach(entry -> map.addProperty(entry.getKey(), entry.getValue()));
                json.add(group.key(), map);
            }
        }
        return json;
    }

    /** Reads what {@link #toJson()} wrote; unknown keys and malformed values are skipped. */
    public static Counters fromJson(JsonObject json) {
        Counters counters = new Counters();
        for (Stat stat : Stat.values()) {
            long value = number(json.get(stat.key()));
            if (value > 0) {
                counters.add(stat, value);
            }
        }
        for (Group group : Group.values()) {
            JsonElement element = json.get(group.key());
            if (element != null && element.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                    long value = number(entry.getValue());
                    if (value > 0 && !entry.getKey().isBlank()) {
                        counters.add(group, entry.getKey(), value);
                    }
                }
            }
        }
        return counters;
    }

    static long number(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return element.getAsLong();
    }
}
