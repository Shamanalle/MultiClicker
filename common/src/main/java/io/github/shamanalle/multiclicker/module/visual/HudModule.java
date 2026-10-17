package io.github.shamanalle.multiclicker.module.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Gfx;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Client;
import io.github.shamanalle.multiclicker.gui.Draw;
import io.github.shamanalle.multiclicker.gui.HudEditScreen;
import io.github.shamanalle.multiclicker.gui.Theme;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.session.GoalsModule;
import io.github.shamanalle.multiclicker.module.clicker.ClickChannel;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.stats.Counters;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;
import io.github.shamanalle.multiclicker.stats.Statistics;
import io.github.shamanalle.multiclicker.util.ServerStats;

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
    /** Distance from the screen edge of a panel in a corner, and where a dragged panel snaps. */
    public static final int MARGIN = 4;
    /** {@link #posX} and {@link #posY} when the panel sits in its {@link #corner}. */
    public static final int IN_CORNER = -1;

    public final EnumSetting<Corner> corner = add(new EnumSetting<>("corner", Corner.TOP_LEFT));
    /**
     * Where the panel was dragged to, in thousandths of the room the screen leaves around it, so
     * that it stays in place when the window is resized; {@link #IN_CORNER} when it was not.
     */
    public final IntSetting posX = add(new IntSetting("pos_x", IN_CORNER, IN_CORNER, 1000, Unit.NONE).internal());
    public final IntSetting posY = add(new IntSetting("pos_y", IN_CORNER, IN_CORNER, 1000, Unit.NONE).internal());
    public final IntSetting scale = add(new IntSetting("scale", 100, 50, 200, Unit.PERCENT));
    public final BoolSetting showWhenInactive = add(new BoolSetting("show_inactive", true));
    public final BoolSetting showProfile = add(new BoolSetting("show_profile", true));
    public final BoolSetting showStats = add(new BoolSetting("show_stats", true));
    public final BoolSetting showRates = add(new BoolSetting("show_rates", false).visibleWhen(showStats::get));
    public final BoolSetting showServer = add(new BoolSetting("show_server", false));
    public final BoolSetting showModules = add(new BoolSetting("show_modules", true));

    /** Where the panel was drawn last, in screen pixels; for the position editor. */
    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;

    public HudModule() {
        super("hud", Category.VISUAL, true, true);
        corner.onChange(this::placeInCorner);
    }

    @Override
    public List<Action> actions() {
        return List.of(new Action("edit_position", parent -> Screens.open(Minecraft.getInstance(), new HudEditScreen(parent))));
    }

    /** Puts the panel back into its corner. */
    public void placeInCorner() {
        posX.set(IN_CORNER);
        posY.set(IN_CORNER);
    }

    public boolean isInCorner() {
        return posX.get() == IN_CORNER || posY.get() == IN_CORNER;
    }

    /**
     * A line of the panel: a label on the left and a value on the right, or a divider. A line with
     * a {@code progress} of 0..1 has a bar under its text.
     */
    private record Line(String left, int leftColor, String right, int rightColor, boolean divider, float progress) {
        Line(String left, int leftColor, String right, int rightColor, boolean divider) {
            this(left, leftColor, right, rightColor, divider, -1);
        }

        static Line separator() {
            return new Line("", 0, "", 0, true);
        }

        int height() {
            return divider ? 5 : progress >= 0 ? LINE_HEIGHT + 4 : LINE_HEIGHT;
        }
    }

    public void render(Canvas g) {
        Minecraft mc = Minecraft.getInstance();
        MultiClicker mod = MultiClicker.get();
        if (!isEnabled() || mod == null || mc.player == null || Screens.hudHidden(mc)
                || Screens.debugShown(mc) || Screens.current(mc) instanceof HudEditScreen) {
            return;
        }
        boolean active = mod.isActive();
        if (!active && !showWhenInactive.get()) {
            return;
        }
        draw(g, mc, mod, active);
    }

    /** Draws the panel for the position editor, whether or not it would be shown now. */
    public void renderPreview(Canvas g) {
        Minecraft mc = Minecraft.getInstance();
        MultiClicker mod = MultiClicker.get();
        draw(g, mc, mod, mod.isActive());
    }

    /** The panel as drawn last: x, y, width and height in screen pixels. */
    public int[] lastBounds() {
        return new int[] {lastX, lastY, lastWidth, lastHeight};
    }

    /** The top-left corner of a panel of the given (scaled) size on a screen of the given size. */
    public int[] position(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        int roomX = Math.max(0, screenWidth - panelWidth);
        int roomY = Math.max(0, screenHeight - panelHeight);
        if (!isInCorner()) {
            return new int[] {Math.round(roomX * posX.get() / 1000.0F), Math.round(roomY * posY.get() / 1000.0F)};
        }
        int x = switch (corner.get()) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> roomX - MARGIN;
        };
        int y = switch (corner.get()) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> roomY - MARGIN;
        };
        return new int[] {x, y};
    }

    /** Moves the panel so that its top-left corner is at the given point. */
    public void moveTo(int x, int y, int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        int roomX = Math.max(1, screenWidth - panelWidth);
        int roomY = Math.max(1, screenHeight - panelHeight);
        posX.set(Math.round(Math.max(0, Math.min(1, x / (float) roomX)) * 1000));
        posY.set(Math.round(Math.max(0, Math.min(1, y / (float) roomY)) * 1000));
    }

    private void draw(Canvas g, Minecraft mc, MultiClicker mod, boolean active) {
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
            height += line.height();
        }

        float factor = scale.get() / 100.0F;
        int scaledWidth = Math.round(width * factor);
        int scaledHeight = Math.round(height * factor);
        int[] position = position(g.width(), g.height(), scaledWidth, scaledHeight);
        int x = position[0];
        int y = position[1];
        lastX = x;
        lastY = y;
        lastWidth = scaledWidth;
        lastHeight = scaledHeight;

        Gfx.push(g, x, y, factor);
        drawPanel(g, font, lines, width, height, active);
        Gfx.pop(g);
    }

    private void drawPanel(Canvas g, Font font, List<Line> lines, int width, int height, boolean active) {
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
            if (line.progress >= 0) {
                int barW = width - PADDING * 2 - 2;
                Draw.rect(g, PADDING + 2, y + 10, barW, 2, 1, Theme.CONTROL);
                int filled = Math.round(barW * line.progress);
                if (filled > 0) {
                    Draw.rect(g, PADDING + 2, y + 10, filled, 2, 1, line.progress >= 1 ? Theme.SUCCESS : accent);
                }
            }
            y += line.height();
        }
    }

    private List<Line> buildLines(Minecraft mc, MultiClicker mod, boolean active) {
        List<Line> lines = new ArrayList<>();
        String status;
        int statusColor;
        if (!active) {
            status = I18n.get("multiclicker.hud.off");
            statusColor = Theme.TEXT_MUTED;
        } else if (Screens.current(mc) != null) {
            status = I18n.get("multiclicker.hud.paused");
            statusColor = Theme.WARNING;
        } else {
            status = I18n.get("multiclicker.hud.on");
            statusColor = Theme.SUCCESS;
        }
        lines.add(new Line("MultiClicker", Theme.accent(), status, statusColor, false));
        String profile = mod.config().activeProfile();
        if (showProfile.get() && profile != null) {
            lines.add(new Line(I18n.get("multiclicker.hud.profile"), Theme.TEXT_DIM, profile, Theme.TEXT, false));
        }
        GoalsModule goals = mod.goals();
        if (active && goals.isEnabled()) {
            float progress = goals.progress();
            lines.add(new Line(I18n.get("multiclicker.hud.goal", I18n.get(goals.goal.get().stat.translationKey())),
                    Theme.TEXT_DIM, goals.progressText(), progress >= 1 ? Theme.SUCCESS : Theme.TEXT, false, progress));
        }

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
            Statistics stats = mod.stats();
            Counters session = stats.session();
            lines.add(Line.separator());
            lines.add(stat("multiclicker.hud.cps", Integer.toString(stats.clicksPerSecond())));
            lines.add(counter(session, Stat.ATTACKS, true));
            lines.add(counter(session, Stat.KILLS, true));
            // The rest only once there is something to show, so a fishing session does not list combat.
            for (Stat stat : List.of(Stat.DAMAGE, Stat.CATCHES, Stat.CROPS, Stat.BLOCKS, Stat.XP)) {
                if (session.get(stat) > 0 || stat == Stat.DAMAGE && session.get(Stat.DAMAGE_HITS) > 0) {
                    lines.add(counter(session, stat, stat != Stat.XP));
                }
            }
            lines.add(stat("multiclicker.hud.time", Statistics.formatDuration(stats.elapsedMillis())));
        }

        if (showServer.get()) {
            lines.add(Line.separator());
            int ping = ServerStats.ping(mc);
            float tps = ServerStats.tps();
            lines.add(stat("multiclicker.hud.ping", ping < 0 ? "—" : ping + " ms"));
            lines.add(stat("multiclicker.hud.tps", tps < 0 ? "—" : String.format(Locale.ROOT, "%.1f", tps)));
            lines.add(stat("multiclicker.hud.fps", Integer.toString(Client.fps(mc))));
        }

        if (showModules.get()) {
            List<Line> moduleLines = new ArrayList<>();
            for (Module module : mod.modules()) {
                // Helpers that act on their own; rule modules (target filter, mining rules) and visuals are not listed.
                boolean listed = module.enabledSetting() != null && module.isEnabled() && module != mod.mining()
                        && module.category() != Category.VISUAL && module.category() != Category.COMBAT
                        && module.category() != Category.SESSION;
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

    /** A counter of the session, with its rate per hour when that is wanted. */
    private Line counter(Counters session, Stat stat, boolean rate) {
        String value = StatFormat.value(session, stat);
        String perHour = rate && showRates.get() ? StatFormat.perHour(session, stat) : "";
        return stat(stat.translationKey(), perHour.isEmpty() ? value : value + "  " + perHour);
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
        double cps = channel.averageCps();
        return String.format(Locale.ROOT, cps >= 10 || cps == Math.floor(cps) ? "%.0f CPS" : "%.1f CPS", cps);
    }
}
