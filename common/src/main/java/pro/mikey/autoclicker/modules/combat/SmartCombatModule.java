package pro.mikey.autoclicker.modules.combat;

import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Smart Combat — intelligent combat features beyond basic clicking.
 * Attack method selector, smart activation, looting swap, camera lock, safety.
 */
public class SmartCombatModule implements Module {

    // ── Enums ──
    public enum AttackMethod {
        LEGIT("Legit"), DIRECT("Direct");

        public final String label;

        AttackMethod(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum LowHpAction {
        STOP("Стоп"), DISCONNECT("Дисконнект");

        public final String label;

        LowHpAction(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ── Метод атаки (hero banner — always visible at the top) ──
    public final EnumSetting<AttackMethod> attackMethod = new EnumSetting<>("smart_combat.attack_method", "Метод атаки",
            AttackMethod.LEGIT, AttackMethod.class)
            .withGroup("__hero__")
            .withDescription(
                    "Legit = имитация мыши (KeyMapping). Direct = пакет gameMode.attack(). Direct видим серверным античитам");

    // ── Умная атака ──
    public final BooleanSetting smartTrigger = new BooleanSetting("smart_combat.smart_trigger", "Smart Активация",
            false)
            .withGroup("🎯 Умная атака")
            .withDescription("Кликер атакует только при N+ мобах в радиусе. Экономит ресурсы на пустых участках");
    public final IntSetting smartTriggerCount = new IntSetting("smart_combat.smart_trigger_count", "Мин. мобов", 5,
            1, 20)
            .withGroup("🎯 Умная атака")
            .withDescription("Минимум мобов в зоне видимости для активации кликера");
    public final BooleanSetting lootingSwapper = new BooleanSetting("smart_combat.looting_swapper", "Looting Swap",
            false)
            .withGroup("🎯 Умная атака")
            .withDescription("Автоматически меняет на меч с Looting перед убийством для макс. лута");

    // ── Safety ──
    public final BooleanSetting safetyEnabled = new BooleanSetting("smart_combat.safety_enabled", "Безопасность", false)
            .withGroup("❤ Безопасность")
            .withDescription("Включает систему защиты HP. Когда HP ниже порога — выполняет выбранное действие");
    public final IntSetting healthThreshold = new IntSetting("smart_combat.health_threshold", "Порог HP %", 0, 0, 100)
            .withGroup("❤ Безопасность")
            .withDescription("Ниже этого % HP сработает действие Low HP. 0 = выключено");
    public final EnumSetting<LowHpAction> lowHpAction = new EnumSetting<>("smart_combat.low_hp_action",
            "Действие при Low HP", LowHpAction.STOP, LowHpAction.class)
            .withGroup("❤ Безопасность")
            .withDescription("Стоп = прекращает атаку. Дисконнект = мгновенный выход с сервера");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(attackMethod, smartTrigger, smartTriggerCount, lootingSwapper,
                safetyEnabled, healthThreshold, lowHpAction);
    }

    @Override
    public String getId() {
        return "smart_combat";
    }

    @Override
    public String getDisplayName() {
        return "Smart Combat";
    }

    @Override
    public Category getCategory() {
        return Category.COMBAT;
    }

    @Override
    public int tickPriority() {
        return 200;
    }
}
