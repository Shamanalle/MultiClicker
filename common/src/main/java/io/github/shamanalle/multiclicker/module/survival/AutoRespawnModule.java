package io.github.shamanalle.multiclicker.module.survival;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Messages;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.visual.NotificationsModule;
import io.github.shamanalle.multiclicker.setting.BoolSetting;

/**
 * On death: stops the mod, writes where the player died into the chat and respawns. Runs whether
 * the mod is on or off, like auto reconnect; never respawns in hardcore.
 */
public class AutoRespawnModule extends Module {
    private static final int RESPAWN_DELAY = 20;

    public final BoolSetting respawn = add(new BoolSetting("respawn", true));
    public final BoolSetting coordinates = add(new BoolSetting("coordinates", true));

    private boolean wasDead;
    private int deadTicks;

    public AutoRespawnModule() {
        super("auto_respawn", Category.SURVIVAL, true, false);
    }

    /** Called every client tick, after the modules; notifications have already reported the death. */
    public void watch(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null) {
            wasDead = false;
            return;
        }
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            deadTicks = 0;
            died(mc, player);
        }
        wasDead = dead;
        if (!dead) {
            if (Screens.current(mc) instanceof DeathScreen) {
                Screens.open(mc, null);
            }
            return;
        }
        if (respawn.get() && !mc.level.getLevelData().isHardcore() && ++deadTicks == RESPAWN_DELAY) {
            player.respawn();
        }
    }

    private void died(Minecraft mc, LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        if (mod.isActive()) {
            mod.setActive(false, Component.translatable("multiclicker.message.died"));
        }
        if (coordinates.get()) {
            Messages.chat(player, Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                    .append(NotificationsModule.deathPlace(player).copy().withStyle(ChatFormatting.YELLOW)));
        }
    }
}
