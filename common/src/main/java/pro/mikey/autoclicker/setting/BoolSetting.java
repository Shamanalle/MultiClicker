package pro.mikey.autoclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    public void toggle() {
        set(!value);
    }

    @Override
    public String displayValue() {
        return I18n.get(value ? "options.on" : "options.off");
    }

    @Override
    protected Boolean sanitize(Boolean value) {
        return value != null && value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
            set(json.getAsBoolean());
        }
    }
}
