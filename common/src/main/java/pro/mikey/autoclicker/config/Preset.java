package pro.mikey.autoclicker.config;

import net.minecraft.network.chat.Component;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.clicker.ClickChannel;
import pro.mikey.autoclicker.module.clicker.ClickerModule;
import pro.mikey.autoclicker.module.mining.MiningModule;
import pro.mikey.autoclicker.setting.Setting;

import java.util.Locale;

/** Ready-made setups. Each preset starts from the defaults, keeping the interface and HUD settings. */
public enum Preset {
    MOB_FARM, MINING, FISHING, DEFAULTS;

    public Component title() {
        return Component.translatable("multiclicker.preset." + name().toLowerCase(Locale.ROOT));
    }

    public Component description() {
        return Component.translatable("multiclicker.preset." + name().toLowerCase(Locale.ROOT) + ".desc");
    }

    public void apply(MultiClicker mod) {
        mod.modules().stream()
                .filter(module -> module != mod.ui() && module != mod.hud())
                .forEach(module -> module.settings().forEach(Setting::reset));
        ClickerModule clicker = mod.clicker();
        switch (this) {
            case MOB_FARM -> {
                clicker.attack.enabled.set(true);
                clicker.attack.jitter.set(2);
                mod.targetFilter().passive.set(false);
                mod.autoEat().enabledSetting().set(true);
                mod.antiAfk().enabledSetting().set(true);
                mod.antiAfk().jump.set(false);
            }
            case MINING -> {
                clicker.attack.enabled.set(true);
                clicker.attack.mode.set(ClickChannel.Mode.HOLD);
                clicker.attackTarget.set(ClickerModule.AttackTarget.ENTITIES_AND_BLOCKS);
                mod.mining().stopWhenFull.set(true);
                mod.mining().filterMode.set(MiningModule.FilterMode.OFF);
                mod.autoTool().enabledSetting().set(true);
                mod.autoEat().enabledSetting().set(true);
            }
            case FISHING -> {
                clicker.attack.enabled.set(false);
                mod.autoFish().enabledSetting().set(true);
                mod.autoEat().enabledSetting().set(true);
                mod.antiAfk().enabledSetting().set(true);
            }
            case DEFAULTS -> {
            }
        }
    }
}
