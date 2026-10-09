package io.github.shamanalle.multiclicker.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Turns a profile into a short line of text to share (compressed JSON in Base64, starting with
 * {@value #PREFIX}) and back. Plain JSON, e.g. a profile file pasted as it is, is read too.
 */
public final class ProfileCodec {
    public static final String PREFIX = "MC1:";
    /** Larger texts are refused: a profile is a few kilobytes. */
    static final int MAX_SIZE = 256 * 1024;
    private static final Gson GSON = new Gson();

    private ProfileCodec() {
    }

    /** A profile read back from text: its name (may be empty) and the settings. */
    public record Decoded(String name, JsonObject config) {
    }

    public static String encode(String name, JsonObject config) {
        JsonObject copy = config.deepCopy();
        copy.addProperty("name", name);
        byte[] json = GSON.toJson(copy).getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(json);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }

    /**
     * Reads a shared profile.
     *
     * @throws IllegalArgumentException if the text is not a MultiClicker profile
     */
    public static Decoded decode(String text) {
        String trimmed = text == null ? "" : text.strip();
        String json;
        if (trimmed.startsWith(PREFIX)) {
            json = inflate(trimmed.substring(PREFIX.length()).strip());
        } else if (trimmed.startsWith("{") && trimmed.length() <= MAX_SIZE) {
            json = trimmed;
        } else {
            throw new IllegalArgumentException("Not a MultiClicker profile");
        }
        JsonElement element;
        try {
            element = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Malformed profile", e);
        }
        if (!element.isJsonObject() || !element.getAsJsonObject().has("modules")
                || !element.getAsJsonObject().get("modules").isJsonObject()) {
            throw new IllegalArgumentException("Not a MultiClicker profile");
        }
        JsonObject root = element.getAsJsonObject();
        JsonElement name = root.remove("name");
        String profileName = name != null && name.isJsonPrimitive() ? name.getAsString() : "";
        return new Decoded(profileName, root);
    }

    private static String inflate(String base64) {
        byte[] compressed;
        try {
            compressed = Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed profile", e);
        }
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int read = inflater.inflate(buffer);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new IllegalArgumentException("Truncated profile");
                }
                out.write(buffer, 0, read);
                if (out.size() > MAX_SIZE) {
                    throw new IllegalArgumentException("Profile too large");
                }
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Malformed profile", e);
        } finally {
            inflater.end();
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
