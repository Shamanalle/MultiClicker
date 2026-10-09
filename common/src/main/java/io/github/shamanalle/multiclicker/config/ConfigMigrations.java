package io.github.shamanalle.multiclicker.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Upgrades saved settings (the main config, profiles and imported profiles) to the current format,
 * so that renamed or reinterpreted settings keep what the player chose.
 *
 * <p>Format history: 1 had no version field, 2 is MultiClicker 2.0 and 2.1, 3 is 2.2 (choice of
 * random delay). A file from a newer version is read as far as it is understood.</p>
 */
public final class ConfigMigrations {
    public static final int CURRENT = 3;
    private static final String[] CHANNELS = {"attack", "use", "jump"};

    private ConfigMigrations() {
    }

    /** The format version of a saved file; files from before versioning count as 1. */
    public static int version(JsonObject root) {
        JsonElement version = root.get("version");
        if (version != null && version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber()) {
            return version.getAsInt();
        }
        return 1;
    }

    /**
     * Upgrades the JSON in place. A file from a newer version is left as it is.
     *
     * @return the version the file had
     */
    public static int migrate(JsonObject root) {
        int version = version(root);
        if (version > CURRENT) {
            return version;
        }
        if (version < 3) {
            keepUniformDelay(root);
        }
        root.addProperty("version", CURRENT);
        return version;
    }

    /**
     * Version 3 added the kind of random delay and made "natural" the default. Settings from before
     * keep the even spread they always had.
     */
    private static void keepUniformDelay(JsonObject root) {
        JsonObject clicker = module(root, "clicker");
        if (clicker == null) {
            return;
        }
        for (String channel : CHANNELS) {
            if (!clicker.has(channel + "_jitter_type")) {
                clicker.addProperty(channel + "_jitter_type", "UNIFORM");
            }
        }
    }

    private static JsonObject module(JsonObject root, String id) {
        JsonElement modules = root.get("modules");
        if (modules == null || !modules.isJsonObject()) {
            return null;
        }
        JsonElement module = modules.getAsJsonObject().get(id);
        return module != null && module.isJsonObject() ? module.getAsJsonObject() : null;
    }
}
