package io.github.shamanalle.multiclicker.boot;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Picks the copy of the mod built for the running loader and Minecraft version.
 *
 * <p>A multi-version jar holds one copy (variant) of the mod per loader and group of Minecraft versions, each
 * under its own package {@code io.github.shamanalle.multiclicker.v.<variant>}. {@code variants.properties},
 * written when the jar is put together, maps {@code <loader>.<minecraft version>} to the variant and lists the
 * entry classes and mixins of each variant.
 */
public final class Variants {
    public static final String PACKAGE = "io.github.shamanalle.multiclicker.v.";
    static final Logger LOGGER = LoggerFactory.getLogger("MultiClicker");
    private static final Properties PROPERTIES = load();
    private static String minecraft;

    private Variants() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = Variants.class.getResourceAsStream("variants.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("MultiClicker: cannot read variants.properties", e);
        }
        return properties;
    }

    /** fabric, neoforge or forge; NeoForged's Forge for Minecraft 1.20.1 still runs Forge's API and counts as forge. */
    public static String loader() {
        if (exists("net.neoforged.fml.loading.FMLLoader")) {
            return "neoforge";
        }
        if (exists("net.minecraftforge.fml.loading.FMLLoader")) {
            return "forge";
        }
        return "fabric";
    }

    private static boolean exists(String name) {
        try {
            Class.forName(name, false, Variants.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static synchronized String minecraft() {
        if (minecraft == null) {
            minecraft = detectMinecraft();
        }
        return minecraft;
    }

    private static String detectMinecraft() {
        String version = fmlMinecraft("net.neoforged");
        if (version == null) {
            version = fmlMinecraft("net.minecraftforge");
        }
        if (version == null) {
            version = fabricMinecraft();
        }
        return version;
    }

    /** FMLLoader.versionInfo().mcVersion(); FML 10 (NeoForge for 26.x) moved it to FMLLoader.getCurrent().getVersionInfo(). */
    private static String fmlMinecraft(String root) {
        try {
            Class<?> loader = Class.forName(root + ".fml.loading.FMLLoader", false, Variants.class.getClassLoader());
            Object info;
            try {
                info = loader.getMethod("versionInfo").invoke(null);
            } catch (NoSuchMethodException e) {
                Object current = loader.getMethod("getCurrent").invoke(null);
                info = loader.getMethod("getVersionInfo").invoke(current);
            }
            return (String) info.getClass().getMethod("mcVersion").invoke(info);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    private static String fabricMinecraft() {
        try {
            ClassLoader classLoader = Variants.class.getClassLoader();
            Class<?> loader = Class.forName("net.fabricmc.loader.api.FabricLoader", false, classLoader);
            Object instance = loader.getMethod("getInstance").invoke(null);
            Optional<?> container = (Optional<?>) loader.getMethod("getModContainer", String.class).invoke(instance, "minecraft");
            if (!container.isPresent()) {
                return null;
            }
            Object metadata = Class.forName("net.fabricmc.loader.api.ModContainer", false, classLoader)
                    .getMethod("getMetadata").invoke(container.get());
            Object version = Class.forName("net.fabricmc.loader.api.metadata.ModMetadata", false, classLoader)
                    .getMethod("getVersion").invoke(metadata);
            return (String) Class.forName("net.fabricmc.loader.api.Version", false, classLoader)
                    .getMethod("getFriendlyString").invoke(version);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    /** The variant for this loader and Minecraft version, or null when the jar has none. */
    public static String current() {
        return PROPERTIES.getProperty(loader() + "." + minecraft());
    }

    public static String property(String variant, String key) {
        return PROPERTIES.getProperty(variant + "." + key);
    }

    /** The variant whose classes are in the given package or one of its subpackages. */
    public static String variantOf(String packageName) {
        if (!packageName.startsWith(PACKAGE)) {
            return null;
        }
        String rest = packageName.substring(PACKAGE.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    /** Whether the game runs as a client; Forge and NeoForge also load the mod on dedicated servers. */
    static boolean client() {
        for (String root : new String[] {"net.neoforged", "net.minecraftforge"}) {
            try {
                Class<?> environment = Class.forName(root + ".fml.loading.FMLEnvironment", false, Variants.class.getClassLoader());
                Object dist;
                try {
                    dist = environment.getField("dist").get(null);
                } catch (NoSuchFieldException e) {
                    dist = environment.getMethod("getDist").invoke(null);
                }
                return "CLIENT".equals(((Enum<?>) dist).name());
            } catch (ClassNotFoundException e) {
                // Not this loader.
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                LOGGER.error("MultiClicker cannot tell whether this is a client", e);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the entry object of the running variant with the constructor that takes the most of the given
     * arguments (each parameter is filled with the first argument of its type), or returns null when the jar has
     * no variant for this game.
     */
    static Object create(String entry, Object... arguments) {
        String variant = current();
        if (variant == null) {
            LOGGER.error("MultiClicker has no build for Minecraft " + minecraft() + " on " + loader());
            return null;
        }
        try {
            Class<?> type = Class.forName(PACKAGE + variant + "." + property(variant, entry), true, Variants.class.getClassLoader());
            Constructor<?>[] constructors = type.getConstructors();
            Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));
            for (int i = constructors.length - 1; i >= 0; i--) {
                Object[] values = fill(constructors[i].getParameterTypes(), arguments);
                if (values != null) {
                    return constructors[i].newInstance(values);
                }
            }
            throw new IllegalStateException("No usable constructor in " + type.getName());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MultiClicker cannot start its build for Minecraft " + minecraft(), e);
        }
    }

    private static Object[] fill(Class<?>[] types, Object[] arguments) {
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            for (Object argument : arguments) {
                if (types[i].isInstance(argument)) {
                    values[i] = argument;
                    break;
                }
            }
            if (values[i] == null) {
                return null;
            }
        }
        return values;
    }

    /** Calls a public static method found by name, or returns null when it does not exist or fails. */
    static Object call(String className, String method) {
        try {
            Method found = Class.forName(className, true, Variants.class.getClassLoader()).getMethod(method);
            return found.invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }
}
