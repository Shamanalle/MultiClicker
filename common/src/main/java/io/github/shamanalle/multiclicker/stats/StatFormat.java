package io.github.shamanalle.multiclicker.stats;

import java.util.Locale;

/** Short text for numbers on the HUD and in the statistics screen. */
public final class StatFormat {
    private StatFormat() {
    }

    /** 999, 1.2k, 12k, 1.2M... */
    public static String compact(double value) {
        double abs = Math.abs(value);
        if (abs < 1000) {
            return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
        }
        String[] suffixes = {"k", "M", "G", "T"};
        int index = -1;
        while (abs >= 1000 && index < suffixes.length - 1) {
            abs /= 1000;
            value /= 1000;
            index++;
        }
        return (abs < 10 ? String.format(Locale.ROOT, "%.1f", value) : Long.toString(Math.round(value))) + suffixes[index];
    }

    /** The value of a counter as shown: time as a duration, damage in health points. */
    public static String value(Counters counters, Stat stat) {
        return switch (stat) {
            case ACTIVE_TIME, COMBAT_TIME -> Statistics.formatDuration(counters.get(stat));
            case DAMAGE -> Statistics.damageKnown(counters) ? compact(Statistics.damage(counters)) : "?";
            default -> compact(counters.get(stat));
        };
    }

    /** The rate per hour, e.g. {@code 1.2k/h}, or empty while too early to tell. */
    public static String perHour(Counters counters, Stat stat) {
        double rate = Statistics.perHour(counters, stat);
        if (rate < 0 || stat == Stat.DAMAGE && !Statistics.damageKnown(counters)) {
            return "";
        }
        return compact(rate < 10 ? Math.round(rate * 10) / 10.0 : Math.round(rate)) + "/h";
    }
}
