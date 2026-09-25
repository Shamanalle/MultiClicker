package pro.mikey.autoclicker.modules.combat;

import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Network Guard — server-side network condition checks.
 * Blocks attacks when ping or TPS are beyond safe limits.
 */
public class NetworkGuardModule implements Module {

    // ── Primary toggle (header pill) ──
    public final BooleanSetting enabled = new BooleanSetting("network.enabled", "Сетевая защита", false)
            .withDescription("Включает сетевую защиту: лимиты пинга/TPS и синхронизацию кликов с сервером");

    // ── Settings ──
    public final IntSetting pingLimit = new IntSetting("network.ping_limit", "Лимит пинга", 0, 0, 500)
            .withDescription("Не атакует при пинге выше этого значения. 0 = без лимита");
    public final FloatSetting tpsLimit = new FloatSetting("network.tps_limit", "Лимит TPS", 0, 0, 20)
            .withDescription("Не атакует при TPS сервера ниже этого. 0 = без лимита");
    public final BooleanSetting tpsSync = new BooleanSetting("network.tps_sync", "Синхронизация с TPS", false)
            .withDescription("Подстраивает частоту кликов под TPS сервера. При лагах — реже бьёт");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, pingLimit, tpsLimit, tpsSync);
    }

    @Override
    public String getId() {
        return "network_guard";
    }

    @Override
    public String getDisplayName() {
        return "Сетевая защита";
    }

    @Override
    public Category getCategory() {
        return Category.PROTECTION;
    }

    @Override
    public int tickPriority() {
        return 200;
    }
}
