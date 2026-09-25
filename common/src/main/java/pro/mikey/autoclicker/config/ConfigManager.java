package pro.mikey.autoclicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.Setting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores all settings in {@code config/multiclicker.json} and named profiles in
 * {@code config/multiclicker/profiles/<name>.json}.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_NAME_LENGTH = 32;

    private final Path file;
    private final Path profilesDir;
    private final List<Module> modules;

    public ConfigManager(Path configDir, List<Module> modules) {
        this.file = configDir.resolve("multiclicker.json");
        this.profilesDir = configDir.resolve("multiclicker").resolve("profiles");
        this.modules = modules;
    }

    public void load() {
        if (Files.exists(file)) {
            read(file);
        }
    }

    public void save() {
        write(file);
    }

    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules) {
            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                values.add(setting.key(), setting.toJson());
            }
            modulesJson.add(module.id(), values);
        }
        root.add("modules", modulesJson);
        return root;
    }

    public void apply(JsonObject root) {
        if (!root.has("modules") || !root.get("modules").isJsonObject()) {
            return;
        }
        JsonObject modulesJson = root.getAsJsonObject("modules");
        for (Module module : modules) {
            JsonElement values = modulesJson.get(module.id());
            if (values == null || !values.isJsonObject()) {
                continue;
            }
            for (Setting<?> setting : module.settings()) {
                JsonElement value = values.getAsJsonObject().get(setting.key());
                if (value != null) {
                    setting.fromJson(value);
                }
            }
        }
    }

    public void resetAll() {
        for (Module module : modules) {
            module.settings().forEach(Setting::reset);
        }
    }

    // --- Profiles -------------------------------------------------------------------------------

    public List<String> profiles() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(profilesDir)) {
            return names;
        }
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(names::add);
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to list profiles", e);
        }
        return names;
    }

    public boolean saveProfile(String name) {
        String clean = sanitizeName(name);
        return !clean.isEmpty() && write(profilesDir.resolve(clean + ".json"));
    }

    public boolean loadProfile(String name) {
        Path path = profilesDir.resolve(sanitizeName(name) + ".json");
        return Files.exists(path) && read(path);
    }

    public void deleteProfile(String name) {
        try {
            Files.deleteIfExists(profilesDir.resolve(sanitizeName(name) + ".json"));
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to delete profile {}", name, e);
        }
    }

    /** Keeps letters (any script), digits, spaces, '-' and '_' so the name is a safe file name. */
    public static String sanitizeName(String name) {
        String clean = name.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        return clean.length() > MAX_NAME_LENGTH ? clean.substring(0, MAX_NAME_LENGTH).trim() : clean;
    }

    // --- IO -------------------------------------------------------------------------------------

    private boolean read(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonObject()) {
                apply(json.getAsJsonObject());
                return true;
            }
            MultiClicker.LOGGER.warn("Ignoring malformed config {}", path);
        } catch (Exception e) {
            MultiClicker.LOGGER.error("Failed to read {}", path, e);
        }
        return false;
    }

    /** Writes to a temporary file first so a crash never leaves a half-written config. */
    private boolean write(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(serialize(), writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to write {}", path, e);
            return false;
        }
    }
}
