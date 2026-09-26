package pro.mikey.autoclicker.setting;

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
