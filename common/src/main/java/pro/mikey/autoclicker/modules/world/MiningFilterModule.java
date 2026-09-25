package pro.mikey.autoclicker.modules.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Mining Filter: whitelist/blacklist for block breaking.
 */
public class MiningFilterModule implements Module {

    public enum FilterMode {
        WHITELIST("Whitelist"), BLACKLIST("Blacklist");

        public final String label;

        FilterMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final BooleanSetting enabled = new BooleanSetting("mining_filter.enabled", "Фильтр блоков", false)
            .withDescription("Ограничивает автоклик ЛКМ по определённым блокам через whitelist/blacklist");
    public final EnumSetting<FilterMode> mode = new EnumSetting<>("mining_filter.mode", "Режим", FilterMode.BLACKLIST,
            FilterMode.class)
            .withDescription("Whitelist = ломает только из списка. Blacklist = ломает всё, кроме списка");
    public final StringListSetting blockList = new StringListSetting("mining_filter.blocks", "Список блоков",
            new java.util.ArrayList<>())
            .withDescription("Идентификаторы блоков (diamond_ore, stone, cobblestone). Подстроковое совпадение");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, mode, blockList);
    }

    @Override
    public String getId() {
        return "mining_filter";
    }

    @Override
    public String getDisplayName() {
        return "Фильтр блоков";
    }

    @Override
    public Category getCategory() {
        return Category.WORLD;
    }

    @Override
    public int tickPriority() {
        return 200;
    }

    /**
     * Returns true if the given block is allowed to be broken.
     */
    public boolean allows(BlockState state) {
        if (!enabled.get())
            return true;
        if (blockList.get().isEmpty())
            return mode.get() == FilterMode.BLACKLIST; // blacklist empty → allow

        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        boolean inList = blockList.get().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(blockId::contains);
        return (mode.get() == FilterMode.WHITELIST) ? inList : !inList;
    }
}
