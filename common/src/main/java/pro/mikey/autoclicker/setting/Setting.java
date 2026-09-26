package pro.mikey.autoclicker.setting;

import com.google.gson.JsonElement;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pro.mikey.autoclicker.module.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * A single configurable value owned by a {@link Module}.
 *
 * <p>Names and descriptions are resolved from the language files using the key
 * {@code multiclicker.module.<module>.<setting>} (plus {@code .desc} for the tooltip),
 * so every setting is fully translatable.</p>
 */
public abstract class Setting<T> {
    private final String key;
    private final T defaultValue;
    protected T value;

    private Module module;
    private BooleanSupplier visibility = () -> true;
    private final List<Runnable> listeners = new ArrayList<>();

    protected Setting(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final void attach(Module module) {
        this.module = module;
    }

    public String key() {
        return key;
    }

    public Module module() {
        return module;
    }

    public String translationKey() {
        return "multiclicker.module." + module.id() + "." + key;
    }

    public Component name() {
        return Component.translatable(translationKey());
    }

    @Nullable
    public Component description() {
        String descKey = translationKey() + ".desc";
        return I18n.exists(descKey) ? Component.translatable(descKey) : null;
    }

    public T get() {
        return value;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (!Objects.equals(sanitized, value)) {
            value = sanitized;
            listeners.forEach(Runnable::run);
        }
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isDefault() {
        return Objects.equals(value, defaultValue);
    }

    /** Hides the setting in the UI unless the condition holds (e.g. options of a disabled mode). */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return (S) this;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    /** Human readable value, used by the UI. */
    public abstract String displayValue();

    protected abstract T sanitize(T value);

    public abstract JsonElement toJson();

    /** Reads a value leniently; malformed input keeps the current value. */
    public abstract void fromJson(JsonElement json);
}
