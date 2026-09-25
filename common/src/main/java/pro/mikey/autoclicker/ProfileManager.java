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

public class ProfileManager {
    private static final Path PROFILES_DIR = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config/autoclicker-profiles");

    public static void init() {
        try {
            if (!Files.exists(PROFILES_DIR)) {
                Files.createDirectories(PROFILES_DIR);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveProfile(String name) {
        if (name == null || name.trim().isEmpty())
            return;

        // Save current config to profile file
        AutoClicker.getInstance().saveConfig(); // Ensure current config is saved to disk first?
        // Actually we want to save the in-memory config to a new file.
        // We can reuse AutoClicker's GSON logic but writing to a different file.

        File profileFile = PROFILES_DIR.resolve(name + ".json").toFile();
        try {
            // We need access to AutoClicker's save logic or reimplement it.
            // Let's add a saveTo(Path) method in AutoClicker or Config.
            AutoClicker.getInstance().saveConfigTo(profileFile);
        } catch (Exception e) {
            e.printStackTrace();
            // Show error message to user?
        }
    }

    public static void loadProfile(String name) {
        File profileFile = PROFILES_DIR.resolve(name + ".json").toFile();
        if (!profileFile.exists())
            return;

        try {
            AutoClicker.getInstance().loadConfigFrom(profileFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteProfile(String name) {
        Path profilePath = PROFILES_DIR.resolve(name + ".json");
        try {
            Files.deleteIfExists(profilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty())
            return;
        Path oldPath = PROFILES_DIR.resolve(oldName + ".json");
        Path newPath = PROFILES_DIR.resolve(newName + ".json");
        try {
            if (Files.exists(oldPath) && !Files.exists(newPath)) {
                Files.move(oldPath, newPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
