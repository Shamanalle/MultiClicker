package io.github.shamanalle.multiclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;
    private final Unit unit;
    @Nullable
    private String zeroKey;
    private boolean logScale;

    public IntSetting(String key, int defaultValue, int min, int max, Unit unit) {
        super(key, defaultValue);
        this.min = min;
        this.max = max;
        this.unit = unit;
    }

    /** Displays the given translation instead of "0" (e.g. "Off" or "Unlimited"). */
    public IntSetting zeroMeans(String translationKey) {
        this.zeroKey = translationKey;
        return this;
    }

    /** The slider covers the range logarithmically, for ranges such as 1 to 100 000. */
    public IntSetting logScale() {
        this.logScale = true;
        return this;
    }

    /** Where a value sits on the slider, 0..1. */
    public double fraction(int value) {
        if (logScale && min > 0) {
            return Math.log((double) value / min) / Math.log((double) max / min);
        }
        return (double) (value - min) / Math.max(1, max - min);
    }

    /** The value at a point of the slider, 0..1; on a log scale rounded to two significant digits. */
    public int valueAt(double fraction) {
        fraction = Mth.clamp(fraction, 0.0, 1.0);
        if (logScale && min > 0) {
            return Mth.clamp((int) roundSignificant(min * Math.pow((double) max / min, fraction)), min, max);
        }
        return (int) Math.round(min + fraction * (max - min));
    }

    /** The value one arrow-key step away; {@code big} for Shift. */
    public int step(int value, int direction, boolean big) {
        if (!logScale) {
            return value + direction * (big ? 10 : 1);
        }
        double factor = big ? 2.0 : 1.1;
        long next = roundSignificant(direction > 0 ? value * factor : value / factor);
        if (next == value) {
            next = value + direction;
        }
        return (int) Mth.clamp(next, min, max);
    }

    private static long roundSignificant(double value) {
        if (value < 100) {
            return Math.round(value);
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)) - 1);
        return Math.round(Math.round(value / magnitude) * magnitude);
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public String displayValue() {
        return format(value);
    }

    /** Formats an arbitrary value of this setting, e.g. to measure the widest label. */
    public String format(int value) {
        if (value == 0 && zeroKey != null) {
            return I18n.get(zeroKey);
        }
        return unit.format(value);
    }

    @Override
    protected Integer sanitize(Integer value) {
        return Mth.clamp(value == null ? defaultValue() : value, min, max);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            set(json.getAsInt());
        }
    }
}
