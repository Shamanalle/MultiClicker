package pro.mikey.autoclicker.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A list of item or block identifiers, e.g. {@code minecraft:cobblestone}. */
public class ListSetting extends Setting<List<String>> {
    public enum Kind {
        ITEM, BLOCK
    }

    private final Kind kind;

    public ListSetting(String key, Kind kind, List<String> defaultValue) {
        super(key, List.copyOf(defaultValue));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean add(String rawId) {
        String id = normalize(rawId);
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

    public boolean contains(ResourceLocation id) {
        return value.contains(id.toString());
    }

    public boolean contains(Item item) {
        return contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public boolean contains(Block block) {
        return contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** Whether the id refers to an existing item/block (unknown ids are highlighted in the UI). */
    public boolean isKnown(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return false;
        }
        return kind == Kind.ITEM
                ? BuiltInRegistries.ITEM.containsKey(location)
                : BuiltInRegistries.BLOCK.containsKey(location);
    }

    /** Lower-cases the id and adds the {@code minecraft:} namespace when omitted. */
    public static String normalize(String raw) {
        String id = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (id.isEmpty()) {
            return "";
        }
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
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
        Set<String> unique = new LinkedHashSet<>();
        for (String entry : value) {
            String id = normalize(entry);
            if (!id.isEmpty()) {
                unique.add(id);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(unique));
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
