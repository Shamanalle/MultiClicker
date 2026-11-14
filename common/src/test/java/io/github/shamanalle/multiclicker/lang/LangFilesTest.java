package io.github.shamanalle.multiclicker.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LangFilesTest {
    private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:%|(?:(\\d+)\\$)?([a-zA-Z]))");

    @Test
    void allTranslationsMatchKeysAndValidFormatSpecifiers() throws IOException {
        Path dir = resolveLangDir();
        assertNotNull(dir, "Language directory not found from " + Path.of("").toAbsolutePath());

        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        assertTrue(files.size() >= 13, "Expected at least 13 translation files, found " + files.size());

        Map<String, Map<String, String>> locales = new LinkedHashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> entries = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                assertTrue(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString(),
                        name + " key " + entry.getKey() + " must be a string");
                String value = entry.getValue().getAsString();
                assertFalse(value.isBlank(), name + " key " + entry.getKey() + " is blank");
                entries.put(entry.getKey(), value);
            }
            locales.put(name, entries);
        }

        Map<String, String> english = locales.get("en_us.json");
        assertNotNull(english, "en_us.json missing");
        assertTrue(locales.containsKey("ru_ru.json"), "ru_ru.json missing");

        for (Map.Entry<String, Map<String, String>> locale : locales.entrySet()) {
            String file = locale.getKey();
            Map<String, String> map = locale.getValue();

            Set<String> missing = new TreeSet<>(english.keySet());
            missing.removeAll(map.keySet());
            assertTrue(missing.isEmpty(), file + " is missing keys: " + missing);

            Set<String> extra = new TreeSet<>(map.keySet());
            extra.removeAll(english.keySet());
            assertTrue(extra.isEmpty(), file + " has unexpected keys: " + extra);

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                int count = validateAndCountPlaceholders(file, key, value);
                int englishCount = validateAndCountPlaceholders("en_us.json", key, english.get(key));
                assertEquals(englishCount, count, file + " key " + key + " has mismatched placeholder count");

                if (value.indexOf('%') >= 0) {
                    Object[] args = new Object[Math.max(10, count)];
                    Arrays.fill(args, "x");
                    try {
                        String.format(Locale.ROOT, value, args);
                    } catch (IllegalFormatException e) {
                        fail(file + " key " + key + " failed String.format: " + e.getMessage() + " (" + value + ")");
                    }
                }
            }
        }
    }

    private static Path resolveLangDir() {
        Path base = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && base != null; i++) {
            Path direct = base.resolve("src/main/resources/assets/multiclicker/lang");
            if (Files.isDirectory(direct)) {
                return direct;
            }
            Path underCommon = base.resolve("common/src/main/resources/assets/multiclicker/lang");
            if (Files.isDirectory(underCommon)) {
                return underCommon;
            }
            base = base.getParent();
        }
        return null;
    }

    private static int validateAndCountPlaceholders(String file, String key, String value) {
        int positionalCount = 0;
        int sequentialCount = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '%') {
                continue;
            }
            Matcher matcher = FORMAT_TOKEN.matcher(value);
            if (!matcher.find(i) || matcher.start() != i) {
                fail(file + " key " + key + " has invalid or unescaped '%' at index " + i + ": " + value);
            }
            if (!"%".equals(matcher.group(0).substring(matcher.group(0).length() - 1)) || matcher.group(2) != null) {
                if (!"s".equals(matcher.group(2))) {
                    fail(file + " key " + key + " uses unsupported format specifier '" + matcher.group(0) + "': " + value);
                }
                if (matcher.group(1) != null) {
                    positionalCount = Math.max(positionalCount, Integer.parseInt(matcher.group(1)));
                } else {
                    sequentialCount++;
                }
            }
            i = matcher.end() - 1;
        }
        return Math.max(positionalCount, sequentialCount);
    }
}
