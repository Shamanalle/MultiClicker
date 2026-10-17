package io.github.shamanalle.multiclicker.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/** A line of text, such as a webhook address. Edited in a dialog opened from the menu row. */
public class StringSetting extends Setting<String> {
    private final int maxLength;
    private boolean secret;
    private Predicate<String> validator = text -> true;
    @Nullable
    private Function<String, CompletableFuture<Component>> tester;

    public StringSetting(String key, String defaultValue, int maxLength) {
        super(key, defaultValue);
        this.maxLength = maxLength;
    }

    /** Hides most of the value in the menu, e.g. a webhook address that works as a password. */
    public StringSetting secret() {
        this.secret = true;
        return this;
    }

    /** Values the editor accepts; the empty value is always accepted. */
    public StringSetting validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    /** Adds a "Test" button to the editor; the future completes with the result to show. */
    public StringSetting tester(Function<String, CompletableFuture<Component>> tester) {
        this.tester = tester;
        return this;
    }

    public int maxLength() {
        return maxLength;
    }

    public boolean isSecret() {
        return secret;
    }

    public boolean isValid(String text) {
        return text.isEmpty() || validator.test(text);
    }

    @Nullable
    public Function<String, CompletableFuture<Component>> tester() {
        return tester;
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    @Override
    public String displayValue() {
        if (value.isEmpty()) {
            return I18n.get("multiclicker.gui.text.empty");
        }
        if (secret) {
            return value.length() <= 12 ? "••••••" : value.substring(0, 12) + "••••••";
        }
        return value;
    }

    @Override
    protected String sanitize(String value) {
        if (value == null) {
            return defaultValue();
        }
        String trimmed = value.strip();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
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
