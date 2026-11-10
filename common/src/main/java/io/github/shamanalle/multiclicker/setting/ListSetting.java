package io.github.shamanalle.multiclicker.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import io.github.shamanalle.multiclicker.compat.Ids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A list of item/block identifiers (e.g. {@code minecraft:cobblestone}) or player names. */
public class ListSetting extends Setting<List<String>> {
    public enum Kind {
        ITEM, BLOCK, PLAYER
    }

    private final Kind kind;

    public ListSetting(String key, Kind kind, List<String> defaultValue) {
        super(key, sanitizeList(defaultValue, kind));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean add(String rawId) {
        String id = normalizeEntry(rawId);
        if (id.isEmpty() || value.contains(id)) {
            return false;
        }
        List<String> copy = new ArrayList<>(value);
        copy.add(id);
        set(copy);
        return true;
    }

    public void remove(String id) {
        List<String> copy = new ArrayList<>(value);
        if (copy.remove(id)) {
            set(copy);
        }
    }

    public boolean contains(String id) {
        return value.contains(id);
    }

    public boolean contains(Item item) {
        return contains(Ids.itemId(item));
    }

    public boolean contains(Block block) {
        return contains(Ids.blockId(block));
    }

    public boolean containsPlayer(String playerName) {
        return playerName != null && contains(normalize(playerName, Kind.PLAYER));
    }

    /** Whether the entry refers to an existing item/block or a valid player username. */
    public boolean isKnown(String id) {
        return switch (kind) {
            case ITEM -> Ids.itemExists(id);
            case BLOCK -> Ids.blockExists(id);
            case PLAYER -> isValidPlayerName(id);
        };
    }

    /** Normalizes {@code raw} according to this list's {@link Kind}. */
    public String normalizeEntry(String raw) {
        return normalize(raw, kind);
    }

    /** Lower-cases an item/block id and adds the {@code minecraft:} namespace when omitted. */
    public static String normalize(String raw) {
        return normalize(raw, Kind.ITEM);
    }

    public static String normalize(String raw, Kind kind) {
        if (raw == null) {
            return "";
        }
        if (kind == Kind.PLAYER) {
            return raw.trim().toLowerCase(Locale.ROOT);
        }
        String id = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (id.isEmpty()) {
            return "";
        }
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }

    private static boolean isValidPlayerName(String name) {
        if (name.isEmpty() || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sanitizeList(List<String> entries, Kind kind) {
        Set<String> unique = new LinkedHashSet<>();
        for (String entry : entries) {
            String id = normalize(entry, kind);
            if (!id.isEmpty()) {
                unique.add(id);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(unique));
    }

    @Override
    public String displayValue() {
        return I18n.get("multiclicker.gui.list_entries", value.size());
    }

    @Override
    protected List<String> sanitize(List<String> value) {
        if (value == null) {
            return defaultValue();
        }
        return sanitizeList(value, kind);
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        value.forEach(array::add);
        return array;
    }

    @Override
    public void fromJson(JsonElement json) {
        if (!json.isJsonArray()) {
            return;
        }
        List<String> entries = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray()) {
            if (element.isJsonPrimitive()) {
                entries.add(element.getAsString());
            }
        }
        set(entries);
    }
}
