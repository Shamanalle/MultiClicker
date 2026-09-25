package pro.mikey.autoclicker.module.visual;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.Theme;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.module.clicker.ClickChannel;
import pro.mikey.autoclicker.module.clicker.ClickerModule;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.ServerStats;
import pro.mikey.autoclicker.util.SessionStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact on-screen panel with the state of the mod. */
public class HudModule extends Module {
    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 11;
    private static final int MARGIN = 4;

    public final EnumSetting<Corner> corner = add(new EnumSetting<>("corner", Corner.TOP_LEFT));
    public final IntSetting scale = add(new IntSetting("scale", 100, 50, 200, Unit.PERCENT));
    public final BoolSetting showWhenInactive = add(new BoolSetting("show_inactive", true));
    public final BoolSetting showStats = add(new BoolSetting("show_stats", true));
    public final BoolSetting showServer = add(new BoolSetting("show_server", false));
    public final BoolSetting showModules = add(new BoolSetting("show_modules", true));

    public HudModule() {
        super("hud", Category.VISUAL, true, true);
    }

    private record Line(String left, int leftColor, String right, int rightColor, boolean divider) {
        static Line separator() {
            return new Line("", 0, "", 0, true);
        }
    }

    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        MultiClicker mod = MultiClicker.get();
        if (!isEnabled() || mod == null || mc.player == null || mc.options.hideGui
                || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        boolean active = mod.isActive();
        if (!active && !showWhenInactive.get()) {
            return;
        }

        List<Line> lines = buildLines(mc, mod, active);
        Font font = mc.font;
        int width = 0;
        for (Line line : lines) {
            int lineWidth = font.width(line.left) + (line.right.isEmpty() ? 0 : font.width(line.right) + 12);
            width = Math.max(width, lineWidth);
        }
        width = Math.max(96, width + PADDING * 2 + 2);
        int height = PADDING * 2 - 2;
        for (Line line : lines) {
            height += line.divider ? 5 : LINE_HEIGHT;
        }

        float factor = scale.get() / 100.0F;
        int scaledWidth = Math.round(width * factor);
        int scaledHeight = Math.round(height * factor);
        int x = switch (corner.get()) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> g.guiWidth() - scaledWidth - MARGIN;
        };
        int y = switch (corner.get()) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> g.guiHeight() - scaledHeight - MARGIN;
        };

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(factor, factor);
        drawPanel(g, font, lines, width, height, active);
        g.pose().popMatrix();
    }

    private void drawPanel(GuiGraphics g, Font font, List<Line> lines, int width, int height, boolean active) {
        int accent = Theme.accent();
        Draw.rect(g, 0, 0, width, height, 3, Theme.HUD_BACKGROUND);
        Draw.rect(g, 0, 0, 2, height, 1, active ? accent : Theme.TEXT_MUTED);
        int y = PADDING;
        for (Line line : lines) {
            if (line.divider) {
                g.fill(PADDING + 2, y + 1, width - PADDING, y + 2, Theme.DIVIDER);
                y += 5;
                continue;
            }
            Draw.text(g, font, line.left, PADDING + 2, y, line.leftColor);
            if (!line.right.isEmpty()) {
                Draw.textRight(g, font, line.right, width - PADDING, y, line.rightColor);
            }
            y += LINE_HEIGHT;
        }
    }

    private List<Line> buildLines(Minecraft mc, MultiClicker mod, boolean active) {
        List<Line> lines = new ArrayList<>();
        String status;
        int statusColor;
        if (!active) {
            status = I18n.get("multiclicker.hud.off");
            statusColor = Theme.TEXT_MUTED;
        } else if (mc.screen != null) {
            status = I18n.get("multiclicker.hud.paused");
            statusColor = Theme.WARNING;
        } else {
            status = I18n.get("multiclicker.hud.on");
            statusColor = Theme.SUCCESS;
        }
        lines.add(new Line("MultiClicker", Theme.accent(), status, statusColor, false));

        ClickerModule clicker = mod.clicker();
        List<ClickChannel> channels = clicker.enabledChannels();
        if (!channels.isEmpty()) {
            lines.add(Line.separator());
            for (ClickChannel channel : channels) {
                lines.add(new Line(I18n.get(channel.hudKey()), Theme.TEXT_DIM, channelInfo(clicker, channel),
                        Theme.TEXT, false));
            }
        }

        if (active && showStats.get()) {
            SessionStats stats = mod.stats();
            lines.add(Line.separator());
            lines.add(stat("multiclicker.hud.cps", Integer.toString(stats.clicksPerSecond())));
            lines.add(stat("multiclicker.hud.attacks", Integer.toString(stats.attacks())));
            lines.add(stat("multiclicker.hud.kills", Integer.toString(stats.kills())));
            lines.add(stat("multiclicker.hud.time", SessionStats.formatDuration(stats.elapsedMillis())));
        }

        if (showServer.get()) {
            lines.add(Line.separator());
            int ping = ServerStats.ping(mc);
            float tps = ServerStats.tps();
            lines.add(stat("multiclicker.hud.ping", ping < 0 ? "—" : ping + " ms"));
            lines.add(stat("multiclicker.hud.tps", tps < 0 ? "—" : String.format(Locale.ROOT, "%.1f", tps)));
            lines.add(stat("multiclicker.hud.fps", Integer.toString(mc.getFps())));
        }

        if (showModules.get()) {
            List<Line> moduleLines = new ArrayList<>();
            for (Module module : mod.modules()) {
                boolean listed = module.enabledSetting() != null && module.isEnabled()
                        && module.category() != Category.VISUAL && module.category() != Category.COMBAT;
                if (!listed) {
                    continue;
                }
                String info = active ? module.hudInfo() : null;
                moduleLines.add(new Line("• " + module.name().getString(), active ? Theme.TEXT : Theme.TEXT_MUTED,
                        info == null ? "" : info, Theme.accent(), false));
            }
            if (!moduleLines.isEmpty()) {
                lines.add(Line.separator());
                lines.addAll(moduleLines);
            }
        }
        return lines;
    }

    private static Line stat(String key, String value) {
        return new Line(I18n.get(key), Theme.TEXT_DIM, value, Theme.TEXT, false);
    }

    private static String channelInfo(ClickerModule clicker, ClickChannel channel) {
        if (channel.mode.get() == ClickChannel.Mode.HOLD) {
            return I18n.get("multiclicker.hud.hold");
        }
        if (channel == clicker.attack && clicker.attackCooldown.get()) {
            return I18n.get("multiclicker.hud.sync");
        }
        double cps = 20.0 / channel.interval.get();
        return String.format(Locale.ROOT, cps >= 10 || cps == Math.floor(cps) ? "%.0f CPS" : "%.1f CPS", cps);
    }
}
