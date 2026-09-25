package pro.mikey.autoclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;

/** Emergency stop for unattended sessions: low health, damage or another player nearby. */
public class SafetyModule extends Module {
    public enum Action {
        STOP, DISCONNECT
    }

    public final EnumSetting<Action> action = add(new EnumSetting<>("action", Action.STOP));
    public final IntSetting health = add(new IntSetting("health", 30, 0, 95, Unit.PERCENT).zeroMeans("options.off"));
    public final BoolSetting onDamage = add(new BoolSetting("on_damage", false));
    public final IntSetting playerRadius = add(new IntSetting("player_radius", 0, 0, 64, Unit.BLOCKS).zeroMeans("options.off"));

    private float lastHealth = -1;

    public SafetyModule() {
        super("safety", Category.SURVIVAL, true, false);
    }

    @Override
    public void start(Minecraft mc) {
        lastHealth = mc.player != null ? mc.player.getHealth() : -1;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) {
            lastHealth = player.getHealth();
            return;
        }
        Component reason = check(mc, player);
        lastHealth = player.getHealth();
        if (reason != null) {
            trigger(mc, reason);
        }
    }

    private Component check(Minecraft mc, LocalPlayer player) {
        float current = player.getHealth();
        if (health.get() > 0 && current / player.getMaxHealth() * 100.0F <= health.get()) {
            return Component.translatable("multiclicker.message.safety.health", Math.round(current));
        }
        if (onDamage.get() && lastHealth >= 0 && current < lastHealth) {
            return Component.translatable("multiclicker.message.safety.damage");
        }
        int radius = playerRadius.get();
        if (radius > 0) {
            for (Player other : mc.level.players()) {
                if (other != player && !other.isSpectator() && other.distanceTo(player) <= radius) {
                    return Component.translatable("multiclicker.message.safety.player", other.getName());
                }
            }
        }
        return null;
    }

    private void trigger(Minecraft mc, Component reason) {
        MultiClicker mod = MultiClicker.get();
        mod.setActive(false, reason);
        if (action.get() == Action.DISCONNECT) {
            Component message = Component.translatable("multiclicker.message.safety.disconnected", reason);
            if (mc.isLocalServer() || mc.getConnection() == null) {
                PauseScreen.disconnectFromWorld(mc, message);
            } else {
                mc.getConnection().getConnection().disconnect(message);
            }
        }
    }
}
