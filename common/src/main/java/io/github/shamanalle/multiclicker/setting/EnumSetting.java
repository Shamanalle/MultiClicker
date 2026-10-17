package io.github.shamanalle.multiclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;

import java.util.Locale;
import java.util.function.Function;

/**
 * Enum setting. Option labels use the key {@code multiclicker.option.<constant>}.
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;
    private Function<E, String> labels = EnumSetting::label;

    public EnumSetting(String key, E defaultValue) {
        super(key, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    /** Shows the options with other labels than {@code multiclicker.option.<constant>}. */
    public EnumSetting<E> labels(Function<E, String> labels) {
        this.labels = labels;
        return this;
    }

    /** The label of any option of this setting. */
    @SuppressWarnings("unchecked")
    public String labelOf(Enum<?> constant) {
        return labels.apply((E) constant);
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
        return labels.apply(value);
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
