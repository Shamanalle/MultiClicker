package pro.mikey.autoclicker;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ProfileManager — manages saving, loading, listing, renaming, and deleting
 * MultiClicker configuration profiles on disk.
 */
public class ProfileManager {
    private static final Path PROFILES_DIR = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config/multiclicker-profiles");

    public static void init() {
        try {
            if (!Files.exists(PROFILES_DIR)) {
                Files.createDirectories(PROFILES_DIR);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to create MultiClicker profiles directory: {}", PROFILES_DIR, e);
        }
    }

    public static List<String> getProfiles() {
        if (!Files.exists(PROFILES_DIR)) {
            init();
        }
        try (Stream<Path> paths = Files.list(PROFILES_DIR)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to list MultiClicker profiles from: {}", PROFILES_DIR, e);
            return new ArrayList<>();
        }
    }

    public static void saveProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        init();
        File profileFile = PROFILES_DIR.resolve(name.trim() + ".json").toFile();
        try {
            AutoClicker.getInstance().saveConfigTo(profileFile);
            AutoClicker.LOGGER.info("Successfully saved profile '{}' to {}", name, profileFile.getAbsolutePath());
        } catch (Exception e) {
            AutoClicker.LOGGER.error("Failed to save profile '{}'", name, e);
        }
    }

    public static void loadProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        File profileFile = PROFILES_DIR.resolve(name.trim() + ".json").toFile();
        if (!profileFile.exists()) {
            AutoClicker.LOGGER.warn("Profile file '{}' does not exist", profileFile.getAbsolutePath());
            return;
        }

        try {
            AutoClicker.getInstance().loadConfigFrom(profileFile);
            AutoClicker.LOGGER.info("Successfully loaded profile '{}'", name);
        } catch (Exception e) {
            AutoClicker.LOGGER.error("Failed to load profile '{}'", name, e);
        }
    }

    public static void deleteProfile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        Path profilePath = PROFILES_DIR.resolve(name.trim() + ".json");
        try {
            Files.deleteIfExists(profilePath);
            AutoClicker.LOGGER.info("Successfully deleted profile '{}'", name);
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to delete profile '{}'", name, e);
        }
    }

    public static void renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return;
        }

        Path oldPath = PROFILES_DIR.resolve(oldName.trim() + ".json");
        Path newPath = PROFILES_DIR.resolve(newName.trim() + ".json");
        try {
            if (Files.exists(oldPath) && !Files.exists(newPath)) {
                Files.move(oldPath, newPath);
                AutoClicker.LOGGER.info("Successfully renamed profile '{}' to '{}'", oldName, newName);
            }
        } catch (IOException e) {
            AutoClicker.LOGGER.error("Failed to rename profile '{}' to '{}'", oldName, newName, e);
        }
    }
}
