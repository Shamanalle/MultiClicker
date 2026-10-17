package io.github.shamanalle.multiclicker.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.clicker.ClickChannel;
import io.github.shamanalle.multiclicker.setting.KeySetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds keys bound to more than one thing: the hotkeys of modules, click channels and profiles,
 * and the vanilla (and other mods') key bindings. Pressing such a key does both things at once.
 */
public final class KeyConflicts {
    /** Something a key is bound to; {@code owner} identifies it, e.g. the {@link KeySetting}. */
    public record Use(Object owner, Component name) {
    }

    /** Key name -> everything bound to it, as of the last {@link #refresh}. */
    private static Map<String, List<Use>> byKey = Map.of();

    private KeyConflicts() {
    }

    /** Collects the bindings again; cheap enough to run every frame while a menu is open. */
    public static void refresh(Minecraft mc) {
        MultiClicker mod = MultiClicker.get();
        Map<String, List<Use>> map = new LinkedHashMap<>();
        for (Module module : mod.modules()) {
            KeySetting key = module.keySetting();
            if (key != null) {
                add(map, key.get(), key, module.name());
            }
        }
        for (ClickChannel channel : mod.clicker().channels()) {
            add(map, channel.key.get(), channel.key, Component.translatable(channel.nameKey()));
        }
        for (String profile : mod.config().profiles()) {
            add(map, mod.config().meta(profile).key(), profile,
                    Component.translatable("multiclicker.gui.conflict.profile", profile));
        }
        for (KeyMapping mapping : mc.options.keyMappings) {
            // Since 1.21.9 the F3 combinations are key bindings too; they only work while F3 is held.
            if (!mapping.isUnbound() && !isDebugCombination(mapping.getName())) {
                add(map, mapping.saveString(), mapping, Component.translatable(mapping.getName()));
            }
        }
        byKey = map;
    }

    private static boolean isDebugCombination(String name) {
        return name.startsWith("key.debug.") && !name.equals("key.debug.overlay") && !name.equals("key.debug.modifier");
    }

    private static void add(Map<String, List<Use>> map, String key, Object owner, Component name) {
        if (key != null && !key.isEmpty() && !key.equals("key.keyboard.unknown")) {
            map.computeIfAbsent(key, k -> new ArrayList<>(2)).add(new Use(owner, name));
        }
    }

    /** What else uses the key of {@code owner}; empty when nothing does. */
    public static List<Component> others(Object owner, String key) {
        List<Use> uses = key == null ? null : byKey.get(key);
        if (uses == null || uses.size() < 2) {
            return List.of();
        }
        List<Component> others = new ArrayList<>();
        for (Use use : uses) {
            if (!use.owner().equals(owner)) {
                others.add(use.name());
            }
        }
        return others;
    }

    public static List<Component> others(KeySetting key) {
        return others(key, key.get());
    }

    /** Every key used by more than one thing, at least one of which belongs to this mod. */
    public static Map<String, List<Use>> all() {
        Map<String, List<Use>> result = new LinkedHashMap<>();
        byKey.forEach((key, uses) -> {
            if (uses.size() > 1 && uses.stream().anyMatch(use -> !(use.owner() instanceof KeyMapping)
                    || ((KeyMapping) use.owner()).getName().startsWith("key.multiclicker."))) {
                result.put(key, uses);
            }
        });
        return result;
    }

    /** Joins names into "A, B, C". */
    public static Component join(List<Component> names) {
        var text = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(names.get(i));
        }
        return text;
    }
}
