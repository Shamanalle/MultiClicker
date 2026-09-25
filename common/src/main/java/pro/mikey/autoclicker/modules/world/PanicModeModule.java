package pro.mikey.autoclicker.modules.world;

import net.minecraft.client.Minecraft;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Panic Mode: auto-disconnect on low HP or player nearby.
 */
public class PanicModeModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("panic.enabled", "Panic Mode", false)
            .withDescription("Экстренный дисконнект при опасности: низкое HP или игрок рядом");
    public final IntSetting hpThreshold = new IntSetting("panic.hp_threshold", "Порог HP %", 15, 1, 99)
            .withDescription("Мгновенный дисконнект при HP ниже этого %. Спасает от смерти на PvP");
    public final IntSetting playerRadius = new IntSetting("panic.player_radius", "Радиус игроков", 0, 0, 64)
            .withDescription("Дисконнект при игроке в радиусе N блоков. 0 = отключено");
    public final BooleanSetting onDamage = new BooleanSetting("panic.on_damage", "При уроне", false)
            .withDescription("Дисконнект при получении урона. Для AFK-ферм где тебя не должны бить");
    public final IntSetting disconnectDelay = new IntSetting("panic.disconnect_delay", "Задержка DC", 0, 0, 20)
            .withDescription("Тиков задержки перед дисконнектом. 2-5 для естественности в серверных логах");

    // State for #9 onDamage and #10 disconnectDelay
    private float lastHealth = -1;
    private int delayCounter = -1;
    private String panicReason = "";

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, hpThreshold, playerRadius, onDamage, disconnectDelay);
    }

    @Override
    public String getId() {
        return "panic_mode";
    }

    @Override
    public String getDisplayName() {
        return "Panic Mode";
    }

    @Override
    public Category getCategory() {
        return Category.PROTECTION;
    }

    @Override
    public int tickPriority() {
        return 10;
    }

    @Override
    public void onDisable() {
        lastHealth = -1;
        delayCounter = -1;
        panicReason = "";
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        // #10 Disconnect delay countdown
        if (delayCounter >= 0) {
            if (delayCounter == 0) {
                AutoClicker instance = AutoClicker.getInstance();
                if (instance != null) {
                    instance.safeDisconnect(mc, panicReason);
                }
                delayCounter = -1;
                return true;
            }
            delayCounter--;
            return false;
        }

        float hp = mc.player.getHealth() / mc.player.getMaxHealth() * 100f;
        boolean lowHp = hp <= hpThreshold.get();
        int radius = playerRadius.get();
        boolean playerNearby = radius > 0 && isPlayerNearby(mc, radius * radius);

        // #9 Damage detection
        boolean tookDamage = false;
        if (onDamage.get()) {
            float currentHealth = mc.player.getHealth();
            if (lastHealth >= 0 && currentHealth < lastHealth) {
                tookDamage = true;
            }
            lastHealth = currentHealth;
        }

        if (lowHp || playerNearby || tookDamage) {
            String reason = tookDamage ? "Panic: Damage taken"
                    : lowHp ? "Panic: Low HP" : "Panic: Player nearby";
            int delay = disconnectDelay.get();
            if (delay > 0) {
                delayCounter = delay;
                panicReason = reason;
                return false;
            }
            AutoClicker instance = AutoClicker.getInstance();
            if (instance != null) {
                instance.safeDisconnect(mc, reason);
            }
            return true;
        }
        return false;
    }

    private boolean isPlayerNearby(Minecraft mc, int radiusSq) {
        if (mc.level == null || mc.player == null)
            return false;
        for (var p : mc.level.players()) {
            if (p == mc.player)
                continue;
            if (p.distanceToSqr(mc.player) <= radiusSq)
                return true;
        }
        return false;
    }
}
