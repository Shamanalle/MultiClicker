package io.github.shamanalle.multiclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.resources.language.I18n;
import io.github.shamanalle.multiclicker.compat.Keys;

/**
 * A hotkey: a keyboard key or mouse button saved by its vanilla name ({@code key.keyboard.g},
 * {@code key.mouse.4}), or nothing. Hotkeys are global: profiles and presets keep them as they are.
 */
public class KeySetting extends Setting<String> {
    private String parsedName;
    private InputConstants.Key parsed = InputConstants.UNKNOWN;

    public KeySetting(String key) {
        super(key, "");
        global();
    }

    /** The bound key, or {@link InputConstants#UNKNOWN} when nothing is bound. */
    public InputConstants.Key bound() {
        if (!value.equals(parsedName)) {
            parsedName = value;
            parsed = Keys.parse(value);
        }
        return parsed;
    }

    public boolean isBound() {
        return !bound().equals(InputConstants.UNKNOWN);
    }

    public boolean matches(InputConstants.Key pressed) {
        return isBound() && bound().equals(pressed);
    }

    public void bind(InputConstants.Key key) {
        set(key.equals(InputConstants.UNKNOWN) ? "" : key.getName());
    }

    public void clear() {
        set("");
    }

    @Override
    public String displayValue() {
        return isBound() ? bound().getDisplayName().getString() : I18n.get("multiclicker.key.none");
    }

    @Override
    protected String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            set(json.getAsString());
        }
    }
}
