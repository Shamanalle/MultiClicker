package io.github.shamanalle.multiclicker.stats;

import java.util.Locale;

/** A counted quantity. The JSON key and translation key derive from the name. */
public enum Stat {
    /** Time the mod was on, in milliseconds. */
    ACTIVE_TIME,
    /** Times the mod was turned on. */
    SESSIONS,
    CLICKS,
    ATTACKS,
    KILLS,
    /** Health taken from other entities, in tenths of a point (half a heart is 10). */
    DAMAGE,
    /** Damage events of ours whose health change was seen; with {@link #DAMAGE_HITS}, tells whether the server hides health. */
    DAMAGE_MEASURED,
    DAMAGE_HITS,
    /** Seconds in which we dealt damage, for the average DPS while fighting. */
    COMBAT_TIME,
    CATCHES,
    FISH,
    TREASURE,
    JUNK,
    OTHER_LOOT,
    CROPS,
    BLOCKS,
    FOOD,
    TOTEMS,
    DEATHS,
    XP,
    DROPPED,
    REFILLS,
    AFK_ACTIONS;

    private final String key = name().toLowerCase(Locale.ROOT);

    /** The JSON key, e.g. {@code active_time}. */
    public String key() {
        return key;
    }

    public String translationKey() {
        return "multiclicker.stats." + key;
    }
}
