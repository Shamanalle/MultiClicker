package pro.mikey.autoclicker.core;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.mikey.autoclicker.AutoClicker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Settings-based configuration manager.
 * Serializes/deserializes module settings as flat JSON:
 * { "module_id.setting_id": value, ... }
 */
public class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger("MultiClicker/ConfigManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ModuleManager moduleManager;
    private Path configPath;

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void setConfigPath(Path path) {
        this.configPath = path;
    }

    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Save all module settings to JSON.
     */
    public void save() {
        if (configPath == null)
            return;
        try {
            JsonObject root = new JsonObject();
            for (Module module : moduleManager.getAll()) {
                List<Setting<?>> settings = module.getSettings();
                if (settings.isEmpty())
                    continue;
                for (Setting<?> setting : settings) {
                    String key = setting.getId();
                    Object val = setting.toSerializable();
                    if (val instanceof Boolean b) {
                        root.addProperty(key, b);
                    } else if (val instanceof Number n) {
                        root.addProperty(key, n);
                    } else if (val instanceof String s) {
                        root.addProperty(key, s);
                    } else if (val instanceof List<?> list) {
                        JsonArray arr = new JsonArray();
                        for (Object item : list)
                            arr.add(String.valueOf(item));
                        root.add(key, arr);
                    }
                }
            }

            // Save global accent color
            AutoClicker instance = AutoClicker.getInstance();
            if (instance != null) {
                root.addProperty("_global.accentColor", instance.getAccentColor());
            }

            Files.createDirectories(configPath.getParent());
            try (Writer writer = new FileWriter(configPath.toFile())) {
                GSON.toJson(root, writer);
            }
            LOGGER.info("Settings saved to {}", configPath);
        } catch (Exception e) {
            LOGGER.error("Failed to save settings", e);
        }
    }

    /**
     * Load settings from JSON and apply to modules.
     */
    public void load() {
        if (configPath == null || !Files.exists(configPath))
            return;
        try (Reader reader = new FileReader(configPath.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null)
                return;

            // Load global accent color
            JsonElement accentElem = root.get("_global.accentColor");
            if (accentElem != null && accentElem.isJsonPrimitive()) {
                AutoClicker instance = AutoClicker.getInstance();
                if (instance != null) {
                    instance.setAccentColor(accentElem.getAsInt());
                }
            }

            for (Module module : moduleManager.getAll()) {
                for (Setting<?> setting : module.getSettings()) {
                    String key = setting.getId();
                    JsonElement element = root.get(key);
                    if (element != null) {
                        applyJsonElement(setting, element);
                    }
                }
            }
            LOGGER.info("Settings loaded from {}", configPath);
        } catch (Exception e) {
            LOGGER.error("Failed to load settings", e);
        }
    }

    private void applyJsonElement(Setting<?> setting, JsonElement element) {
        try {
            if (element.isJsonPrimitive()) {
                JsonPrimitive prim = element.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    setting.setFromObject(prim.getAsBoolean());
                } else if (prim.isNumber()) {
                    setting.setFromObject(prim.getAsNumber());
                } else if (prim.isString()) {
                    setting.setFromObject(prim.getAsString());
                }
            } else if (element.isJsonArray()) {
                java.util.ArrayList<String> list = new java.util.ArrayList<>();
                for (JsonElement item : element.getAsJsonArray()) {
                    list.add(item.getAsString());
                }
                setting.setFromObject(list);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to apply setting {}: {}", setting.getId(), e.getMessage());
        }
    }

    /**
     * Save settings to an arbitrary path (for profiles).
     */
    public void saveTo(Path path) {
        Path original = this.configPath;
        this.configPath = path;
        save();
        this.configPath = original;
    }

    /**
     * Load settings from an arbitrary path (for profiles).
     */
    public void loadFrom(Path path) {
        Path original = this.configPath;
        this.configPath = path;
        load();
        this.configPath = original;
    }
}
