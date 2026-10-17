package io.github.shamanalle.multiclicker.module.session;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Reconnect;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.gui.ReconnectScreen;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Joins the server again after the connection was lost, then loads the server profile and turns
 * the mod back on. Never after a ban, a login from elsewhere, a kick (unless allowed), the safety
 * disconnect or leaving by hand. Works between sessions, so it is watched every tick instead of
 * ticking with the mod.
 */
public class AutoReconnectModule extends Module {
    /** Ticks to let the world load after joining before the mod is turned on again. */
    private static final int RESUME_DELAY = 60;
    private static final Pattern BAN = Pattern.compile("\\bban(ned)?\\b|\\bblacklist|забан|заблокир|\\bbanni|\\bgebannt");
    private static final Pattern KICK = Pattern.compile("\\bkick|кикнут|выгнан");

    public final IntSetting delay = add(new IntSetting("delay", 10, 3, 300, Unit.SECONDS));
    public final IntSetting attempts = add(new IntSetting("attempts", 5, 1, 50, Unit.NONE));
    public final BoolSetting resume = add(new BoolSetting("resume", true));
    public final BoolSetting afterKick = add(new BoolSetting("after_kick", false));

    /** The last multiplayer server joined; kept after the connection is gone. */
    @Nullable
    private ServerData server;
    /** Whether the mod was working when the connection was lost. */
    private boolean wasActive;
    /** The next disconnect is ours (safety): do not come back. */
    private boolean expectDisconnect;
    @Nullable
    private Screen handledScreen;
    private int attempt;
    /** Ticks until the mod is turned on again after a reconnect, or -1. */
    private int resumeIn = -1;
    private boolean reconnecting;

    public AutoReconnectModule() {
        super("auto_reconnect", Category.SESSION, true, false);
    }

    /** The next disconnect was asked for (safety): do not reconnect after it. */
    public void expectDisconnect() {
        expectDisconnect = true;
    }

    /** Every client tick, whether the mod is on or not. */
    public void watch(Minecraft mc) {
        Screen screen = Screens.current(mc);
        if (mc.player != null) {
            ServerData current = mc.getCurrentServer();
            if (current != null && !mc.isLocalServer()) {
                server = current;
            }
            handledScreen = null;
            expectDisconnect = false;
            if (reconnecting) {
                reconnecting = false;
                attempt = 0;
                resumeIn = wasActive && resume.get() ? RESUME_DELAY : -1;
                MultiClicker.get().notifications().reconnected(mc);
            }
            wasActive = MultiClicker.get().isActive();
            if (resumeIn > 0 && --resumeIn == 0 && screen == null) {
                resumeIn = -1;
                MultiClicker.get().setActive(true, null);
            }
            return;
        }
        if (!(screen instanceof DisconnectedScreen disconnected) || screen == handledScreen) {
            return;
        }
        handledScreen = screen;
        Component reason = Reconnect.reason(disconnected);
        boolean retry = isEnabled() && server != null && !expectDisconnect && allowed(reason)
                && attempt < attempts.get();
        if (!reconnecting) {
            // The first loss of this connection; failed attempts are not reported one by one.
            MultiClicker.get().notifications().disconnected(mc, reason, retry);
        }
        expectDisconnect = false;
        if (!retry) {
            reset();
            return;
        }
        attempt++;
        reconnecting = true;
        Screens.open(mc, new ReconnectScreen(this, reason, attempt, attempts.get(), delay.get() * 20));
    }

    /** The countdown ran out or the player pressed "reconnect now". */
    public void connect(Minecraft mc) {
        if (server == null) {
            cancel(mc);
            return;
        }
        Reconnect.connect(mc, serverList(), server);
    }

    /** The player gave up waiting. */
    public void cancel(Minecraft mc) {
        reset();
        Screens.open(mc, serverList());
    }

    private void reset() {
        reconnecting = false;
        attempt = 0;
        resumeIn = -1;
    }

    private static Screen serverList() {
        return new JoinMultiplayerScreen(new TitleScreen());
    }

    /** Whether the server would let us back in (and wants us back). */
    private boolean allowed(Component reason) {
        String key = reason.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
        if (key.contains("banned") || key.contains("duplicate_login") || key.contains("not_whitelisted")) {
            return false;
        }
        String text = reason.getString().toLowerCase(Locale.ROOT);
        if (BAN.matcher(text).find()) {
            return false;
        }
        return afterKick.get() || !key.contains("kicked") && !KICK.matcher(text).find();
    }
}
