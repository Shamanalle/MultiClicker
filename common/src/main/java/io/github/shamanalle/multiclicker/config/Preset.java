package io.github.shamanalle.multiclicker.config;

import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.module.clicker.ClickChannel;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.module.mining.MiningModule;
import io.github.shamanalle.multiclicker.setting.Setting;

import java.util.Locale;

/**
 * Ready-made setups. Each preset starts from the defaults, keeping the interface and HUD settings
 * and the hotkeys.
 */
public enum Preset {
    MOB_FARM, MINING, FISHING, FARMING, AFK_IN_PLACE, DEFAULTS;

    public Component title() {
        return Component.translatable("multiclicker.preset." + name().toLowerCase(Locale.ROOT));
    }

    public Component description() {
        return Component.translatable("multiclicker.preset." + name().toLowerCase(Locale.ROOT) + ".desc");
    }

    public void apply(MultiClicker mod) {
        mod.modules().stream()
                .filter(module -> module != mod.ui() && module != mod.hud())
                .flatMap(module -> module.settings().stream())
                .filter(setting -> !setting.isGlobal())
                .forEach(Setting::reset);
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
            case FARMING -> {
                clicker.attack.enabled.set(false);
                mod.autoFarm().enabledSetting().set(true);
                mod.hotbarRefill().enabledSetting().set(true);
                mod.autoEat().enabledSetting().set(true);
            }
            case AFK_IN_PLACE -> {
                clicker.attack.enabled.set(false);
                mod.antiAfk().enabledSetting().set(true);
                mod.antiAfk().jump.set(false);
                mod.antiAfk().sneak.set(true);
                mod.antiAfk().swing.set(true);
                mod.antiAfk().rotate.set(false);
                mod.antiAfk().step.set(false);
                mod.antiAfk().switchSlot.set(true);
            }
            case DEFAULTS -> {
            }
        }
        mod.config().clearActiveProfile();
    }
}
