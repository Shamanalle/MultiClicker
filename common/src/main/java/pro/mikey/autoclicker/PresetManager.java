package pro.mikey.autoclicker;

import pro.mikey.autoclicker.core.ModuleManager;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.combat.AntiCheatModule;
import pro.mikey.autoclicker.modules.world.MiningModule;
import pro.mikey.autoclicker.modules.world.AutoFishModule;
import pro.mikey.autoclicker.modules.automation.AntiAfkModule;
import pro.mikey.autoclicker.modules.automation.AutoEatModule;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Farm presets — quick-apply predefined configurations.
 * Each preset modifies only settings relevant to its purpose,
 * leaving unrelated settings untouched.
 */
public class PresetManager {

    public record Preset(String id, String icon, String name, String description, Consumer<ModuleManager> applier) {
    }

    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();

    static {
        // ── 🌾 Моб-ферма ──
        register(new Preset("mob_farm", "🌾", "Моб-ферма",
                "ЛКМ 20 CPS, кулдаун вкл, Mob Mode вкл",
                mm -> {
                    CombatClickerModule c = mm.getModule(CombatClickerModule.class);
                    if (c != null) {
                        c.leftActive.set(true);
                        c.leftSpeed.set(0);
                        c.respectCooldown.set(true);
                        c.mobMode.set(true);
                        c.leftRandomize.set(false);
                    }
                }));

        // ── ⛏ Копка ──
        register(new Preset("mining", "⛏", "Копка",
                "Mining Mode вкл, кликер пауза",
                mm -> {
                    MiningModule m = mm.getModule(MiningModule.class);
                    if (m != null) m.miningMode.set(true);
                }));

        // ── 🎣 Рыбалка ──
        register(new Preset("fishing", "🎣", "Рыбалка",
                "Авто-рыбалка, рандомизация вкл",
                mm -> {
                    AutoFishModule f = mm.getModule(AutoFishModule.class);
                    if (f != null) {
                        f.enabled.set(true);
                        f.randomize.set(true);
                    }
                }));

        // ── 💤 AFK-ферма ──
        register(new Preset("afk_farm", "💤", "AFK-ферма",
                "ЛКМ 10 CPS + Anti-AFK + Auto-Eat",
                mm -> {
                    CombatClickerModule c = mm.getModule(CombatClickerModule.class);
                    if (c != null) {
                        c.leftActive.set(true);
                        c.leftSpeed.set(1);
                        c.respectCooldown.set(true);
                        c.mobMode.set(true);
                    }
                    AntiAfkModule a = mm.getModule(AntiAfkModule.class);
                    if (a != null) a.enabled.set(true);
                    AutoEatModule e = mm.getModule(AutoEatModule.class);
                    if (e != null) e.enabled.set(true);
                }));

        // ── 🔒 Серверный ──
        register(new Preset("server_safe", "🔒", "Серверный",
                "Рандомизация + GCD + Skip 5%",
                mm -> {
                    CombatClickerModule c = mm.getModule(CombatClickerModule.class);
                    if (c != null) {
                        c.leftRandomize.set(true);
                        c.leftRandomizeRange.set(4);
                    }
                    AntiCheatModule ac = mm.getModule(AntiCheatModule.class);
                    if (ac != null) {
                        ac.gcdPatch.set(true);
                        ac.skipChance.set(5);
                    }
                }));
    }

    private static void register(Preset preset) {
        PRESETS.put(preset.id(), preset);
    }

    public static Map<String, Preset> getPresets() {
        return PRESETS;
    }

    public static Preset getPreset(String id) {
        return PRESETS.get(id);
    }

    public static void applyPreset(String id) {
        Preset preset = PRESETS.get(id);
        if (preset != null) {
            preset.applier().accept(AutoClicker.getInstance().getModuleManager());
            AutoClicker.getInstance().saveConfig();
        }
    }
}
