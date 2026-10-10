package io.github.shamanalle.multiclicker.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * One finished session, kept in the history.
 *
 * @param start  when the mod was turned on, in epoch milliseconds
 * @param server the server address, {@code singleplayer}, or empty when unknown
 */
public record SessionRecord(long start, String server, Counters counters) {
    /** Entries of each group kept per session. */
    static final int GROUP_LIMIT = 10;

    public long duration() {
        return counters.get(Stat.ACTIVE_TIME);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("start", start);
        json.addProperty("server", server);
        json.add("stats", counters.toJson());
        return json;
    }

    @Nullable
    public static SessionRecord fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        JsonElement stats = json.get("stats");
        if (stats == null || !stats.isJsonObject()) {
            return null;
        }
        JsonElement server = json.get("server");
        return new SessionRecord(Counters.number(json.get("start")),
                server != null && server.isJsonPrimitive() ? server.getAsString() : "",
                Counters.fromJson(stats.getAsJsonObject()));
    }
}
