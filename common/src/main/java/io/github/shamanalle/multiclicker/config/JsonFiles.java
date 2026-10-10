package io.github.shamanalle.multiclicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Reads and writes the mod's JSON files. */
public final class JsonFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonFiles() {
    }

    /** The object in the file, or {@code null} when it is missing, unreadable or not an object. */
    @Nullable
    public static JsonObject read(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonObject()) {
                return json.getAsJsonObject();
            }
            MultiClicker.LOGGER.warn("Ignoring malformed file {}", path);
        } catch (Exception e) {
            MultiClicker.LOGGER.error("Failed to read {}", path, e);
        }
        return null;
    }

    /** Writes to a temporary file first so a crash never leaves a half-written file. */
    public static boolean write(Path path, JsonObject json) {
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

    public static String toString(JsonObject json) {
        return GSON.toJson(json);
    }
}
