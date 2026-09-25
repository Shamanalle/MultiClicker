package pro.mikey.autoclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

/**
 * Enum setting. Option labels use the key {@code multiclicker.option.<constant>}.
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;

    public EnumSetting(String key, E defaultValue) {
        super(key, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] values() {
        return values;
    }

    public void cycle(int direction) {
        int next = Math.floorMod(value.ordinal() + direction, values.length);
        set(values[next]);
    }

    public static String label(Enum<?> constant) {
        return I18n.get("multiclicker.option." + constant.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public String displayValue() {
        return label(value);
    }

    @Override
    protected E sanitize(E value) {
        return value == null ? defaultValue() : value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (!json.isJsonPrimitive()) {
            return;
        }
        String name = json.getAsString();
        for (E candidate : values) {
            if (candidate.name().equalsIgnoreCase(name)) {
                set(candidate);
                return;
            }
        }
    }
}
