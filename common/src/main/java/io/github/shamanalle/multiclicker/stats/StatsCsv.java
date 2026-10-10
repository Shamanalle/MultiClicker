package io.github.shamanalle.multiclicker.stats;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * The statistics as a CSV table for a spreadsheet: one row for all time, one per server and one
 * per session in the history, one column per counter. Times are in seconds, damage in health points.
 */
public final class StatsCsv {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private StatsCsv() {
    }

    public static String write(StatsStore store, ZoneId zone) {
        StringBuilder csv = new StringBuilder("scope,server,start");
        for (Stat stat : Stat.values()) {
            csv.append(',').append(stat.key());
        }
        csv.append('\n');
        row(csv, "total", "", "", store.total());
        for (Map.Entry<String, Counters> server : store.servers().entrySet()) {
            row(csv, "server", server.getKey(), "", server.getValue());
        }
        for (SessionRecord session : store.history()) {
            row(csv, "session", session.server(), DATE.format(Instant.ofEpochMilli(session.start()).atZone(zone)),
                    session.counters());
        }
        return csv.toString();
    }

    private static void row(StringBuilder csv, String scope, String server, String start, Counters counters) {
        csv.append(scope).append(',').append(quote(server)).append(',').append(start);
        for (Stat stat : Stat.values()) {
            long value = counters.get(stat);
            csv.append(',');
            switch (stat) {
                case ACTIVE_TIME, COMBAT_TIME -> csv.append(value / 1000);
                case DAMAGE -> csv.append(String.format(Locale.ROOT, "%.1f", value / 10.0));
                default -> csv.append(value);
            }
        }
        csv.append('\n');
    }

    private static String quote(String text) {
        if (text.indexOf(',') < 0 && text.indexOf('"') < 0 && text.indexOf('\n') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
