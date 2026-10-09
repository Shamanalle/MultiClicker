package io.github.shamanalle.multiclicker.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigMigrationsTest {
    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject clicker(JsonObject root) {
        return root.getAsJsonObject("modules").getAsJsonObject("clicker");
    }

    @Test
    void version2KeepsTheEvenRandomDelay() {
        JsonObject root = parse("{\"version\": 2, \"modules\": {\"clicker\": {\"attack_jitter\": 4}}}");
        assertEquals(2, ConfigMigrations.migrate(root));
        assertEquals(ConfigMigrations.CURRENT, ConfigMigrations.version(root));
        for (String channel : new String[]{"attack", "use", "jump"}) {
            assertEquals("UNIFORM", clicker(root).get(channel + "_jitter_type").getAsString());
        }
        assertEquals(4, clicker(root).get("attack_jitter").getAsInt());
    }

    @Test
    void aChoiceAlreadyMadeIsKept() {
        JsonObject root = parse("{\"version\": 2, \"modules\": {\"clicker\": {\"use_jitter_type\": \"NORMAL\"}}}");
        ConfigMigrations.migrate(root);
        assertEquals("NORMAL", clicker(root).get("use_jitter_type").getAsString());
        assertEquals("UNIFORM", clicker(root).get("attack_jitter_type").getAsString());
    }

    @Test
    void currentFilesAreNotChanged() {
        JsonObject root = parse("{\"version\": 3, \"modules\": {\"clicker\": {\"attack_interval\": 3}}}");
        JsonObject before = root.deepCopy();
        assertEquals(3, ConfigMigrations.migrate(root));
        assertEquals(before, root);
    }

    @Test
    void filesFromANewerVersionAreLeftAlone() {
        JsonObject root = parse("{\"version\": 99, \"modules\": {\"clicker\": {}}}");
        assertEquals(99, ConfigMigrations.migrate(root));
        assertEquals(99, ConfigMigrations.version(root));
        assertFalse(clicker(root).has("attack_jitter_type"));
    }

    @Test
    void filesWithoutVersionOrModulesAreHandled() {
        JsonObject root = parse("{}");
        assertEquals(1, ConfigMigrations.migrate(root));
        assertEquals(ConfigMigrations.CURRENT, ConfigMigrations.version(root));
        JsonObject noClicker = parse("{\"modules\": {\"auto_eat\": {}}}");
        ConfigMigrations.migrate(noClicker);
        assertFalse(noClicker.getAsJsonObject("modules").has("clicker"));
    }
}
