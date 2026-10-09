package io.github.shamanalle.multiclicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.Setting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Stores all settings in {@code config/multiclicker.json} and named profiles in
 * {@code config/multiclicker/profiles/<name>.json}.
 *
 * <p>A profile holds every setting except the global ones (hotkeys). What belongs to a profile but
 * not to its settings, its hotkey and the servers it loads on, is kept in the main file.</p>
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_NAME_LENGTH = 32;
    /** How a single player world is listed among a profile's servers. */
    public static final String SINGLEPLAYER = "singleplayer";

    private final Path file;
    private final Path profilesDir;
    private final List<Module> modules;
    private final Map<String, ProfileMeta> profileMeta = new LinkedHashMap<>();
    @Nullable
    private String activeProfile;
    private boolean applying;

    /** A profile's hotkey and the servers it is loaded on automatically. */
    public static final class ProfileMeta {
        String key = "";
        final List<String> servers = new ArrayList<>();

        public String key() {
            return key;
        }

        public List<String> servers() {
            return List.copyOf(servers);
        }

        boolean isEmpty() {
            return key.isEmpty() && servers.isEmpty();
        }
    }

    public ConfigManager(Path configDir, List<Module> modules) {
        this.file = configDir.resolve("multiclicker.json");
        this.profilesDir = configDir.resolve("multiclicker").resolve("profiles");
        this.modules = modules;
    }

    public void load() {
        JsonObject root = Files.exists(file) ? read(file) : null;
        if (root == null) {
            return;
        }
        int version = ConfigMigrations.migrate(root);
        if (version > ConfigMigrations.CURRENT) {
            // Saved by a newer MultiClicker: keep a copy, since this version drops what it does not know.
            backup(version);
        }
        apply(root, false);
        readMeta(root);
    }

    public void save() {
        JsonObject root = serialize(false);
        writeMeta(root);
        write(file, root);
    }

    /**
     * All settings as JSON.
     *
     * @param profile leave out the global settings (hotkeys), which a profile does not store
     */
    public JsonObject serialize(boolean profile) {
        JsonObject root = new JsonObject();
        root.addProperty("version", ConfigMigrations.CURRENT);
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules) {
            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                if (!(profile && setting.isGlobal())) {
                    values.add(setting.key(), setting.toJson());
                }
            }
            modulesJson.add(module.id(), values);
        }
        root.add("modules", modulesJson);
        return root;
    }

    /**
     * Applies settings read from JSON in the current format; anything missing or malformed keeps
     * its current value.
     *
     * @param profile leave the global settings (hotkeys) as they are
     */
    public void apply(JsonObject root, boolean profile) {
        if (!root.has("modules") || !root.get("modules").isJsonObject()) {
            return;
        }
        JsonObject modulesJson = root.getAsJsonObject("modules");
        applying = true;
        try {
            for (Module module : modules) {
                JsonElement values = modulesJson.get(module.id());
                if (values == null || !values.isJsonObject()) {
                    continue;
                }
                for (Setting<?> setting : module.settings()) {
                    if (profile && setting.isGlobal()) {
                        continue;
                    }
                    JsonElement value = values.getAsJsonObject().get(setting.key());
                    if (value != null) {
                        setting.fromJson(value);
                    }
                }
            }
        } finally {
            applying = false;
        }
    }

    /** True while settings are being loaded, so a change is not the player's own edit. */
    public boolean isApplying() {
        return applying;
    }

    public void resetAll() {
        for (Module module : modules) {
            module.settings().forEach(Setting::reset);
        }
        activeProfile = null;
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

    public boolean hasProfile(String name) {
        String clean = sanitizeName(name);
        return !clean.isEmpty() && Files.exists(profilePath(clean));
    }

    public boolean saveProfile(String name) {
        String clean = sanitizeName(name);
        if (clean.isEmpty() || !write(profilePath(clean), serialize(true))) {
            return false;
        }
        activeProfile = clean;
        return true;
    }

    public boolean loadProfile(String name) {
        String clean = sanitizeName(name);
        Path path = profilePath(clean);
        JsonObject root = Files.exists(path) ? read(path) : null;
        if (root == null) {
            return false;
        }
        ConfigMigrations.migrate(root);
        apply(root, true);
        activeProfile = clean;
        return true;
    }

    public void deleteProfile(String name) {
        String clean = sanitizeName(name);
        try {
            Files.deleteIfExists(profilePath(clean));
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to delete profile {}", name, e);
        }
        profileMeta.remove(clean);
        if (clean.equals(activeProfile)) {
            activeProfile = null;
        }
    }

    /** The profile loaded or saved last, as long as no setting was changed since; {@code null} otherwise. */
    @Nullable
    public String activeProfile() {
        return activeProfile;
    }

    /** Called when the player changes a setting: the settings no longer match the profile. */
    public void clearActiveProfile() {
        activeProfile = null;
    }

    private Path profilePath(String cleanName) {
        return profilesDir.resolve(cleanName + ".json");
    }

    /** Keeps letters (any script), digits, spaces, '-' and '_' so the name is a safe file name. */
    public static String sanitizeName(String name) {
        String clean = name.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        return clean.length() > MAX_NAME_LENGTH ? clean.substring(0, MAX_NAME_LENGTH).trim() : clean;
    }

    // --- Sharing --------------------------------------------------------------------------------

    /** A saved profile as a line of text to share, or {@code null} if it cannot be read. */
    @Nullable
    public String exportProfile(String name) {
        String clean = sanitizeName(name);
        Path path = profilePath(clean);
        JsonObject root = Files.exists(path) ? read(path) : null;
        if (root == null) {
            return null;
        }
        ConfigMigrations.migrate(root);
        return ProfileCodec.encode(clean, root);
    }

    /** The current settings as a line of text to share. */
    public String exportCurrent(String name) {
        return ProfileCodec.encode(sanitizeName(name), serialize(true));
    }

    /**
     * Saves a shared profile under its own name, or the given one if it has none. An existing
     * profile is never overwritten: a number is added to the name instead.
     *
     * @return the name it was saved under
     * @throws IllegalArgumentException if the text is not a MultiClicker profile
     */
    public String importProfile(String text, String fallbackName) {
        ProfileCodec.Decoded decoded = ProfileCodec.decode(text);
        JsonObject root = decoded.config();
        ConfigMigrations.migrate(root);
        // Hotkeys are the player's own: an imported profile never brings any.
        removeGlobalSettings(root);
        String base = sanitizeName(decoded.name());
        if (base.isEmpty()) {
            base = sanitizeName(fallbackName);
        }
        if (base.isEmpty()) {
            base = "Profile";
        }
        String name = base;
        for (int i = 2; hasProfile(name); i++) {
            String suffix = " " + i;
            name = base.substring(0, Math.min(base.length(), MAX_NAME_LENGTH - suffix.length())).trim() + suffix;
        }
        if (!write(profilePath(name), root)) {
            throw new IllegalStateException("Failed to save the profile");
        }
        return name;
    }

    private void removeGlobalSettings(JsonObject root) {
        JsonObject modulesJson = root.getAsJsonObject("modules");
        for (Module module : modules) {
            JsonElement values = modulesJson.get(module.id());
            if (values != null && values.isJsonObject()) {
                for (Setting<?> setting : module.settings()) {
                    if (setting.isGlobal()) {
                        values.getAsJsonObject().remove(setting.key());
                    }
                }
            }
        }
    }

    // --- Profile hotkeys and servers ------------------------------------------------------------

    public ProfileMeta meta(String name) {
        ProfileMeta meta = profileMeta.get(sanitizeName(name));
        return meta != null ? meta : new ProfileMeta();
    }

    public void setProfileKey(String name, String keyName) {
        String clean = sanitizeName(name);
        ProfileMeta meta = profileMeta.computeIfAbsent(clean, n -> new ProfileMeta());
        meta.key = keyName == null ? "" : keyName;
        if (!meta.key.isEmpty()) {
            // One key loads one profile.
            profileMeta.forEach((other, otherMeta) -> {
                if (!other.equals(clean) && otherMeta.key.equals(meta.key)) {
                    otherMeta.key = "";
                }
            });
        }
        profileMeta.values().removeIf(ProfileMeta::isEmpty);
    }

    /** The profile bound to this key name, if any. */
    @Nullable
    public String profileForKey(String keyName) {
        for (Map.Entry<String, ProfileMeta> entry : profileMeta.entrySet()) {
            if (!keyName.isEmpty() && entry.getValue().key.equals(keyName) && hasProfile(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Loads the profile on this server from now on, or stops doing so. A server loads at most one
     * profile, so it is taken away from any other profile.
     *
     * @return whether the profile is now loaded on the server
     */
    public boolean toggleServer(String name, String server) {
        String clean = sanitizeName(name);
        String address = normalizeServer(server);
        ProfileMeta meta = profileMeta.computeIfAbsent(clean, n -> new ProfileMeta());
        boolean added;
        if (meta.servers.remove(address)) {
            added = false;
        } else {
            profileMeta.values().forEach(other -> other.servers.remove(address));
            meta.servers.add(address);
            added = true;
        }
        profileMeta.values().removeIf(ProfileMeta::isEmpty);
        return added;
    }

    /** The profile to load when joining this server, if any. */
    @Nullable
    public String profileForServer(String server) {
        String address = normalizeServer(server);
        for (Map.Entry<String, ProfileMeta> entry : profileMeta.entrySet()) {
            if (entry.getValue().servers.contains(address) && hasProfile(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** "Play.Example.net:25565" and "play.example.net" are the same server. */
    public static String normalizeServer(String address) {
        String clean = address.strip().toLowerCase(Locale.ROOT);
        if (clean.endsWith(":25565")) {
            clean = clean.substring(0, clean.length() - ":25565".length());
        }
        while (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private void readMeta(JsonObject root) {
        profileMeta.clear();
        JsonElement profiles = root.get("profiles");
        if (profiles != null && profiles.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : profiles.getAsJsonObject().entrySet()) {
                String name = sanitizeName(entry.getKey());
                if (name.isEmpty() || !entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject json = entry.getValue().getAsJsonObject();
                ProfileMeta meta = new ProfileMeta();
                if (json.has("key") && json.get("key").isJsonPrimitive()) {
                    meta.key = json.get("key").getAsString();
                }
                if (json.has("servers") && json.get("servers").isJsonArray()) {
                    for (JsonElement server : json.getAsJsonArray("servers")) {
                        if (server.isJsonPrimitive() && !server.getAsString().isBlank()) {
                            meta.servers.add(normalizeServer(server.getAsString()));
                        }
                    }
                }
                if (!meta.isEmpty()) {
                    profileMeta.put(name, meta);
                }
            }
        }
        JsonElement active = root.get("active_profile");
        activeProfile = active != null && active.isJsonPrimitive() && !active.getAsString().isEmpty()
                ? sanitizeName(active.getAsString()) : null;
    }

    private void writeMeta(JsonObject root) {
        JsonObject profiles = new JsonObject();
        profileMeta.forEach((name, meta) -> {
            JsonObject json = new JsonObject();
            json.addProperty("key", meta.key);
            JsonArray servers = new JsonArray();
            meta.servers.forEach(servers::add);
            json.add("servers", servers);
            profiles.add(name, json);
        });
        root.add("profiles", profiles);
        if (activeProfile != null) {
            root.addProperty("active_profile", activeProfile);
        }
    }

    // --- IO -------------------------------------------------------------------------------------

    @Nullable
    private JsonObject read(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonObject()) {
                return json.getAsJsonObject();
            }
            MultiClicker.LOGGER.warn("Ignoring malformed config {}", path);
        } catch (Exception e) {
            MultiClicker.LOGGER.error("Failed to read {}", path, e);
        }
        return null;
    }

    private void backup(int version) {
        Path copy = file.resolveSibling(file.getFileName() + ".v" + version + ".bak");
        try {
            if (!Files.exists(copy)) {
                Files.copy(file, copy);
                MultiClicker.LOGGER.warn("{} is from a newer MultiClicker (format {}); kept a copy as {}", file, version, copy);
            }
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to back up {}", file, e);
        }
    }

    /** Writes to a temporary file first so a crash never leaves a half-written config. */
    private boolean write(Path path, JsonObject json) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
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
