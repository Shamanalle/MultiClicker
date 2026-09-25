package pro.mikey.autoclicker.modules.world;

import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Mining module — a distinct operational mode that overrides the auto-clicker.
 * When active, holds LMB to break blocks continuously.
 * Lives in CLICKER category as a mode switch (top of the tab).
 */
public class MiningModule implements Module {

    public final BooleanSetting miningMode = new BooleanSetting("mining.mining_mode", "⛏ Mining Mode", false)
            .withGroup("⛏ Режим добычи")
            .withDescription("Переключает кликер в режим непрерывной добычи блоков. Все боевые функции паузятся");

    public final BooleanSetting saveToolEnabled = new BooleanSetting("mining.save_tool", "Save Tool", false)
            .withGroup("🛡 Защита инструмента")
            .withDescription("Останавливает копку, когда прочность инструмента низкая. Защита от поломки");
    public final IntSetting saveToolThreshold = new IntSetting("mining.save_tool_threshold", "Порог прочности", 10,
            1, 100)
            .withGroup("🛡 Защита инструмента")
            .withDescription("Минимальная прочность для остановки копки. Ниже — кликер паузится");
    public final BooleanSetting stopWhenFull = new BooleanSetting("mining.stop_when_full", "Стоп при полном инвентаре",
            false)
            .withGroup("⚙ Авто")
            .withDescription("Прекращает копку когда инвентарь полон. Не ломает блоки впустую");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(miningMode, saveToolEnabled, saveToolThreshold, stopWhenFull);
    }

    @Override
    public String getId() {
        return "mining";
    }

    @Override
    public String getDisplayName() {
        return "Режим добычи";
    }

    @Override
    public Category getCategory() {
        return Category.CLICKER;
    }

    @Override
    public int tickPriority() {
        return 200;
    }
}
