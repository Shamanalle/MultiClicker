package pro.mikey.autoclicker.setting;

import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

/** How an integer setting is presented in the UI. */
public enum Unit {
    NONE,
    TICKS,
    /** A delay between clicks, also shown as clicks per second. */
    CLICK_INTERVAL,
    SECONDS,
    MINUTES,
    PERCENT,
    BLOCKS,
    HUNGER;

    public String format(int value) {
        return switch (this) {
            case NONE -> Integer.toString(value);
            case TICKS -> I18n.get("multiclicker.unit.ticks", value, decimal(value / 20.0));
            case CLICK_INTERVAL -> I18n.get("multiclicker.unit.click_interval", value, decimal(20.0 / Math.max(1, value)));
            case SECONDS -> I18n.get("multiclicker.unit.seconds", value);
            case MINUTES -> I18n.get("multiclicker.unit.minutes", value);
            case PERCENT -> value + "%";
            case BLOCKS -> I18n.get("multiclicker.unit.blocks", value);
            case HUNGER -> I18n.get("multiclicker.unit.hunger", value);
        };
    }

    private static String decimal(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        // Trim trailing zeros: 1.50 -> 1.5, 2.00 -> 2
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
