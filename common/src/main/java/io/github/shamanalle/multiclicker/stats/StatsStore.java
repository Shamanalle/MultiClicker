package io.github.shamanalle.multiclicker.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.shamanalle.multiclicker.config.JsonFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The statistics kept between games in {@code config/multiclicker/stats.json}: all-time counters,
 * counters per server and the most recent sessions. Nothing leaves the computer.
 */
public final class StatsStore {
    public static final int HISTORY_SIZE = 20;
    /** Entries of each group kept for all time and per server. */
    static final int GROUP_LIMIT = 100;
    private static final int FORMAT = 1;

    private final Path file;
    private final Counters total = new Counters();
    private final Map<String, Counters> servers = new LinkedHashMap<>();
    /** Newest first. */
    private final List<SessionRecord> history = new ArrayList<>();

    public StatsStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public Counters total() {
        return total;
    }

    /** The counters of a server, created on first use. */
    public Counters server(String address) {
        return servers.computeIfAbsent(address, key -> new Counters());
    }

    public Map<String, Counters> servers() {
        return Collections.unmodifiableMap(servers);
    }

    public List<SessionRecord> history() {
        return Collections.unmodifiableList(history);
    }

    public void addHistory(SessionRecord record) {
        Counters counters = record.counters().copy();
        counters.prune(SessionRecord.GROUP_LIMIT);
        history.add(0, new SessionRecord(record.start(), record.server(), counters));
        while (history.size() > HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
    }

    /** Forgets everything; the caller saves. */
    public void clear() {
        total.clear();
        servers.clear();
        history.clear();
    }

    public void load() {
        JsonObject root = JsonFiles.read(file);
        if (root != null) {
            fromJson(root);
        }
    }

    public boolean save() {
        return JsonFiles.write(file, toJson());
    }

    public JsonObject toJson() {
        total.prune(GROUP_LIMIT);
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.add("total", total.toJson());
        JsonObject serverJson = new JsonObject();
        servers.forEach((address, counters) -> {
            counters.prune(GROUP_LIMIT);
            serverJson.add(address, counters.toJson());
        });
        root.add("servers", serverJson);
        JsonArray historyJson = new JsonArray();
        history.forEach(record -> historyJson.add(record.toJson()));
        root.add("history", historyJson);
        return root;
    }

    void fromJson(JsonObject root) {
        clear();
        JsonElement totalJson = root.get("total");
        if (totalJson != null && totalJson.isJsonObject()) {
            total.addAll(Counters.fromJson(totalJson.getAsJsonObject()));
        }
        JsonElement serverJson = root.get("servers");
        if (serverJson != null && serverJson.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : serverJson.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject() && !entry.getKey().isBlank()) {
                    servers.put(entry.getKey(), Counters.fromJson(entry.getValue().getAsJsonObject()));
                }
            }
        }
        JsonElement historyJson = root.get("history");
        if (historyJson != null && historyJson.isJsonArray()) {
            for (JsonElement element : historyJson.getAsJsonArray()) {
                SessionRecord record = SessionRecord.fromJson(element);
                if (record != null && history.size() < HISTORY_SIZE) {
                    history.add(record);
                }
            }
        }
    }
}
