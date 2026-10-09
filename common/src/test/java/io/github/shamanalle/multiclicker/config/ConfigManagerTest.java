package io.github.shamanalle.multiclicker.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.clicker.Jitter;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.ListSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @TempDir
    Path dir;

    /** Stands in for the clicker: a switch, a hotkey, a list and the random delay kind. */
    static final class TestModule extends Module {
        final BoolSetting flag = add(new BoolSetting("flag", false));
        final ListSetting items = add(new ListSetting("items", ListSetting.Kind.ITEM, List.of()));
        final EnumSetting<Jitter> jitterType = add(new EnumSetting<>("attack_jitter_type", Jitter.NATURAL));

        TestModule() {
            super("clicker", Category.CLICKER, true, false);
        }
    }

    private TestModule module;
    private ConfigManager config;

    @BeforeEach
    void setUp() {
        module = new TestModule();
        config = new ConfigManager(dir, List.of(module));
    }

    private ConfigManager reopen() {
        module = new TestModule();
        config = new ConfigManager(dir, List.of(module));
        config.load();
        return config;
    }

    @Test
    void settingsSurviveARestart() {
        module.flag.set(true);
        module.items.add("cobblestone");
        module.keySetting().set("key.keyboard.g");
        config.save();

        reopen();
        assertTrue(module.flag.get());
        assertEquals(List.of("minecraft:cobblestone"), module.items.get());
        assertEquals("key.keyboard.g", module.keySetting().get());
    }

    @Test
    void profilesDoNotStoreHotkeys() {
        module.flag.set(true);
        module.keySetting().set("key.keyboard.g");
        assertTrue(config.saveProfile("Farm"));
        assertEquals("Farm", config.activeProfile());

        module.flag.set(false);
        module.keySetting().set("key.keyboard.k");
        assertTrue(config.loadProfile("Farm"));
        assertTrue(module.flag.get());
        assertEquals("key.keyboard.k", module.keySetting().get(), "loading a profile changed a hotkey");
    }

    @Test
    void theActiveProfileIsRemembered() {
        config.saveProfile("Farm");
        config.save();
        assertEquals("Farm", reopen().activeProfile());
        config.clearActiveProfile();
        config.save();
        assertNull(reopen().activeProfile());
    }

    @Test
    void aKeyLoadsOneProfile() {
        config.saveProfile("A");
        config.saveProfile("B");
        config.setProfileKey("A", "key.keyboard.f6");
        assertEquals("A", config.profileForKey("key.keyboard.f6"));
        config.setProfileKey("B", "key.keyboard.f6");
        assertEquals("B", config.profileForKey("key.keyboard.f6"));
        assertEquals("", config.meta("A").key());
        config.save();
        assertEquals("B", reopen().profileForKey("key.keyboard.f6"));
    }

    @Test
    void aServerLoadsOneProfile() {
        config.saveProfile("A");
        config.saveProfile("B");
        assertTrue(config.toggleServer("A", "Play.Example.net:25565"));
        assertEquals("A", config.profileForServer("play.example.net"));
        assertTrue(config.toggleServer("B", "play.example.net"));
        assertEquals("B", config.profileForServer("PLAY.EXAMPLE.NET"));
        assertTrue(config.meta("A").servers().isEmpty());
        config.save();
        assertEquals("B", reopen().profileForServer("play.example.net"));
        assertFalse(config.toggleServer("B", "play.example.net"));
        assertNull(config.profileForServer("play.example.net"));
    }

    @Test
    void deletingAProfileForgetsItsKeyAndServers() {
        config.saveProfile("A");
        config.setProfileKey("A", "key.keyboard.f6");
        config.toggleServer("A", ConfigManager.SINGLEPLAYER);
        config.deleteProfile("A");
        assertNull(config.profileForKey("key.keyboard.f6"));
        assertNull(config.profileForServer(ConfigManager.SINGLEPLAYER));
        assertNull(config.activeProfile());
    }

    @Test
    void sharedProfilesAreImportedWithoutHotkeys() throws IOException {
        JsonObject json = JsonParser.parseString(
                "{\"version\": 3, \"modules\": {\"clicker\": {\"flag\": true, \"key\": \"key.keyboard.x\"}}}").getAsJsonObject();
        String text = ProfileCodec.encode("Iron farm", json);
        assertEquals("Iron farm", config.importProfile(text, "ignored"));
        assertEquals("Iron farm 2", config.importProfile(text, "ignored"));
        String saved = Files.readString(dir.resolve("multiclicker/profiles/Iron farm.json"));
        assertFalse(saved.contains("key.keyboard.x"), "an imported profile brought a hotkey");

        module.keySetting().set("key.keyboard.g");
        assertTrue(config.loadProfile("Iron farm"));
        assertTrue(module.flag.get());
        assertEquals("key.keyboard.g", module.keySetting().get());
    }

    @Test
    void importedProfilesWithoutANameUseTheGivenOne() {
        String text = ProfileCodec.encode("", config.serialize(true));
        assertEquals("Mine", config.importProfile(text, "Mine"));
        assertEquals("Profile", config.importProfile(text, ""));
        String longName = "x".repeat(32);
        assertEquals(longName, config.importProfile(ProfileCodec.encode(longName, config.serialize(true)), ""));
        String second = config.importProfile(ProfileCodec.encode(longName, config.serialize(true)), "");
        assertTrue(second.length() <= 32 && second.endsWith(" 2"), second);
        assertThrows(IllegalArgumentException.class, () -> config.importProfile("nonsense", "x"));
    }

    @Test
    void oldConfigsAreMigratedOnLoad() throws IOException {
        Files.writeString(dir.resolve("multiclicker.json"), "{\"version\": 2, \"modules\": {\"clicker\": {\"flag\": true}}}");
        reopen();
        assertTrue(module.flag.get());
        assertEquals(Jitter.UNIFORM, module.jitterType.get(), "an old config must keep the even random delay");
    }

    @Test
    void newConfigsUseTheNewDefault() {
        reopen();
        assertEquals(Jitter.NATURAL, module.jitterType.get());
    }

    @Test
    void aConfigFromANewerVersionIsBackedUp() throws IOException {
        Files.writeString(dir.resolve("multiclicker.json"), "{\"version\": 99, \"modules\": {\"clicker\": {\"flag\": true}}}");
        reopen();
        assertTrue(module.flag.get(), "what this version understands is still read");
        assertTrue(Files.exists(dir.resolve("multiclicker.json.v99.bak")));
    }

    @Test
    void namesAndAddressesAreCleanedUp() {
        assertEquals("Iron farm", ConfigManager.sanitizeName("  Iron farm?!/ "));
        assertEquals("Ферма 2", ConfigManager.sanitizeName("Ферма 2"));
        assertEquals("play.example.net", ConfigManager.normalizeServer(" Play.Example.NET:25565 "));
        assertEquals("play.example.net:25566", ConfigManager.normalizeServer("play.example.net:25566"));
        assertEquals("play.example.net", ConfigManager.normalizeServer("play.example.net."));
    }
}
