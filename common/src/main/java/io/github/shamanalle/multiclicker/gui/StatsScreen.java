package io.github.shamanalle.multiclicker.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Gfx;
import io.github.shamanalle.multiclicker.compat.Ids;
import io.github.shamanalle.multiclicker.compat.Sounds;
import io.github.shamanalle.multiclicker.config.ConfigManager;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.stats.Counters;
import io.github.shamanalle.multiclicker.stats.LootKind;
import io.github.shamanalle.multiclicker.stats.SessionRecord;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;
import io.github.shamanalle.multiclicker.stats.Statistics;
import io.github.shamanalle.multiclicker.stats.StatsCsv;
import io.github.shamanalle.multiclicker.stats.Timeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the mod did: the current session with its graphs, the server being played on, all time,
 * and the last sessions. Everything can be exported as CSV or reset.
 */
public class StatsScreen extends PanelScreen {
    private enum Tab {
        SESSION, SERVER, TOTAL, HISTORY;

        Component title() {
            return Component.translatable("multiclicker.gui.stats.tab." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** A clickable area found while drawing. */
    private record Hit(int x, int y, int w, int h, boolean inView, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }

    private static final long CONFIRM_MS = 3000;
    private static final int TAB_HEIGHT = 16;
    private static final int GAP = 6;
    private static final int TILE_HEIGHT = 36;
    private static final int GRAPH_HEIGHT = 92;
    private static final int TOP_ROW = 18;
    private static final int TOP_ENTRIES = 5;
    private static final int HISTORY_ROW = 28;
    private static final List<Stat> TILES = List.of(Stat.ACTIVE_TIME, Stat.CLICKS, Stat.KILLS, Stat.DAMAGE,
            Stat.CATCHES, Stat.CROPS, Stat.BLOCKS, Stat.XP);
    private static final List<Stat> MORE = List.of(Stat.ATTACKS, Stat.FOOD, Stat.TOTEMS, Stat.DEATHS,
            Stat.DROPPED, Stat.REFILLS, Stat.AFK_ACTIONS, Stat.COMBAT_TIME, Stat.SESSIONS);
    private static final int[] LOOT_COLORS = {0xFF4FA3F7, 0xFFF5C542, 0xFF8A7F6E, 0xFFB57BEA};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private static Tab lastTab = Tab.SESSION;
    private static int lastMetric;

    private final MultiClicker mod = MultiClicker.get();
    private final Statistics stats = mod.stats();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<Tab, Anim> tabHover = new HashMap<>();
    private Tab tab = lastTab;
    /** A session of the history opened from the list. */
    @Nullable
    private SessionRecord openedRecord;
    @Nullable
    private String server;
    private FlatButton resetButton;
    private long resetPendingSince;
    private Component status = Component.empty();
    private int statusColor = Theme.TEXT_MUTED;
    @Nullable
    private Component statusTooltip;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private float scroll;
    private int contentHeight;
    @Nullable
    private Component tooltip;

    public StatsScreen(Screen parent) {
        super(parent, Component.translatable("multiclicker.gui.stats"));
        if (!stats.serverAddress().isEmpty()) {
            server = stats.serverAddress();
        } else if (!stats.store().servers().isEmpty()) {
            server = stats.store().servers().keySet().iterator().next();
        }
    }

    @Override
    protected int preferredWidth() {
        return 560;
    }

    @Override
    protected int preferredHeight() {
        return 340;
    }

    @Override
    protected void init() {
        super.init();
        int x = panelX + panelW - 8;
        int resetW = Math.max(46, Math.max(font.width(I18n.get("multiclicker.gui.stats.reset")),
                font.width(I18n.get("multiclicker.gui.stats.confirm_reset"))) + 16);
        x -= resetW;
        resetButton = button(Component.translatable("multiclicker.gui.stats.reset"), FlatButton.Style.DANGER, this::reset)
                .bounds(x, panelY + 7, resetW, 16);
        int exportW = Math.max(46, font.width(I18n.get("multiclicker.gui.stats.export")) + 16);
        x -= exportW + 4;
        button(Component.translatable("multiclicker.gui.stats.export"), FlatButton.Style.NORMAL, this::export)
                .bounds(x, panelY + 7, exportW, 16);
        int backW = Math.max(46, font.width(I18n.get("gui.back")) + 16);
        x -= backW + 4;
        button(Component.translatable("gui.back"), FlatButton.Style.NORMAL, this::onClose)
                .bounds(x, panelY + 7, backW, 16);
        viewX = panelX + 10;
        viewY = panelY + 36 + TAB_HEIGHT + 8;
        viewW = panelW - 20;
        viewH = panelY + panelH - 8 - viewY;
    }

    // --- Actions --------------------------------------------------------------------------------

    private void export() {
        Path dir = stats.store().file().getParent();
        Path file = dir.resolve("stats-" + FILE_DATE.format(LocalDateTime.now()) + ".csv");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, StatsCsv.write(stats.store(), ZoneId.systemDefault()), StandardCharsets.UTF_8);
            setStatus(Component.translatable("multiclicker.gui.stats.exported", file.getFileName().toString()), Theme.SUCCESS);
            statusTooltip = Component.literal(file.toAbsolutePath().toString());
        } catch (IOException e) {
            MultiClicker.LOGGER.error("Failed to export statistics to {}", file, e);
            setStatus(Component.translatable("multiclicker.gui.stats.export_failed"), Theme.DANGER);
        }
    }

    /** Asks once more before forgetting everything. */
    private void reset() {
        if (resetPendingSince != 0 && System.currentTimeMillis() - resetPendingSince <= CONFIRM_MS) {
            stats.reset();
            resetPendingSince = 0;
            openedRecord = null;
            resetButton.label = Component.translatable("multiclicker.gui.stats.reset");
            setStatus(Component.translatable("multiclicker.gui.stats.reset_done"), Theme.TEXT_MUTED);
        } else {
            resetPendingSince = System.currentTimeMillis();
            resetButton.label = Component.translatable("multiclicker.gui.stats.confirm_reset");
        }
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
        statusTooltip = null;
    }

    private void showTab(Tab next) {
        tab = next;
        lastTab = next;
        openedRecord = null;
        scroll = 0;
    }

    private void cycleServer(int direction) {
        List<String> servers = new ArrayList<>(stats.store().servers().keySet());
        if (servers.isEmpty()) {
            return;
        }
        int index = Math.max(0, servers.indexOf(server));
        server = servers.get(Math.floorMod(index + direction, servers.size()));
        scroll = 0;
    }

    // --- Drawing --------------------------------------------------------------------------------

    @Override
    protected void renderPanel(Canvas g, int mouseX, int mouseY, float delta) {
        hits.clear();
        tooltip = null;
        if (resetPendingSince != 0 && System.currentTimeMillis() - resetPendingSince > CONFIRM_MS) {
            resetPendingSince = 0;
            resetButton.label = Component.translatable("multiclicker.gui.stats.reset");
        }
        drawStatus(g, mouseX, mouseY);
        drawTabs(g, mouseX, mouseY, delta);

        boolean mouseInView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
        int hoverX = mouseInView ? mouseX : Integer.MIN_VALUE;
        int hoverY = mouseInView ? mouseY : Integer.MIN_VALUE;
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - viewH));
        g.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);
        int top = viewY - Math.round(scroll);
        int bottom = switch (tab) {
            case SESSION -> stats.startedAt() == 0
                    ? empty(g, top, "multiclicker.gui.stats.no_session")
                    : overview(g, top, stats.session(), true, hoverX, hoverY);
            case SERVER -> {
                Counters counters = server == null ? null : stats.store().servers().get(server);
                yield counters == null ? empty(g, top, "multiclicker.gui.stats.no_server")
                        : overview(g, top, counters, false, hoverX, hoverY);
            }
            case TOTAL -> overview(g, top, stats.store().total(), false, hoverX, hoverY);
            case HISTORY -> openedRecord != null ? record(g, top, openedRecord, hoverX, hoverY) : history(g, top, hoverX, hoverY);
        };
        g.disableScissor();
        contentHeight = bottom - top;
        drawScrollbar(g);
        if (tooltip != null) {
            Gfx.tooltip(g, font, font.split(tooltip, 240), mouseX, mouseY);
        }
    }

    private void drawStatus(Canvas g, int mouseX, int mouseY) {
        if (status.getString().isEmpty()) {
            return;
        }
        int x = panelX + 15 + font.width(title.copy().withStyle(net.minecraft.ChatFormatting.BOLD)) + 10;
        int right = buttons.stream().mapToInt(button -> button.x).min().orElse(panelX + panelW) - 8;
        String text = Draw.ellipsize(font, status.getString(), right - x);
        Draw.text(g, font, text, x, panelY + 11, statusColor);
        if (statusTooltip != null && mouseX >= x && mouseX < x + font.width(text) && mouseY >= panelY + 8 && mouseY < panelY + 22) {
            tooltip = statusTooltip;
        }
    }

    private void drawTabs(Canvas g, int mouseX, int mouseY, float delta) {
        int x = viewX;
        int y = panelY + 36;
        int accent = Theme.accent();
        for (Tab entry : Tab.values()) {
            String text = entry.title().getString();
            int w = font.width(text) + 16;
            boolean selected = entry == tab;
            boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_HEIGHT;
            float h = tabHover.computeIfAbsent(entry, e -> new Anim(0)).update(over || selected ? 1 : 0, delta, 18);
            int fill = selected ? Theme.alpha(accent, 0.18F) : Theme.alpha(0xFFFFFFFF, 0.05F * h);
            Draw.rect(g, x, y, w, TAB_HEIGHT, 3, fill);
            Draw.textCentered(g, font, text, x + w / 2, y + 4, selected ? accent : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h));
            hits.add(new Hit(x, y, w, TAB_HEIGHT, false, () -> showTab(entry)));
            x += w + 4;
        }
        int right = viewX + viewW;
        if (tab == Tab.SESSION && stats.isRunning()) {
            String live = "● " + I18n.get("multiclicker.gui.stats.live");
            Draw.textRight(g, font, live, right, y + 4, Theme.SUCCESS);
        } else if (tab == Tab.SESSION && stats.startedAt() > 0) {
            Draw.textRight(g, font, I18n.get("multiclicker.gui.stats.last_session"), right, y + 4, Theme.TEXT_MUTED);
        } else if (tab == Tab.SERVER && server != null) {
            int count = stats.store().servers().size();
            String name = Draw.ellipsize(font, serverName(server), Math.max(40, right - x - 40));
            if (count > 1) {
                String arrows = "◀ " + name + " ▶";
                int w = font.width(arrows) + 12;
                int chipX = right - w;
                boolean over = mouseX >= chipX && mouseX < right && mouseY >= y && mouseY < y + TAB_HEIGHT;
                Draw.rect(g, chipX, y, w, TAB_HEIGHT, 3, over ? Theme.CONTROL_HOVER : Theme.CONTROL);
                Draw.textCentered(g, font, arrows, chipX + w / 2, y + 4, Theme.TEXT);
                hits.add(new Hit(chipX, y, w / 2, TAB_HEIGHT, false, () -> cycleServer(-1)));
                hits.add(new Hit(chipX + w / 2, y, w - w / 2, TAB_HEIGHT, false, () -> cycleServer(1)));
                if (over) {
                    tooltip = Component.translatable("multiclicker.gui.stats.switch_server");
                }
            } else {
                Draw.textRight(g, font, name, right, y + 4, Theme.TEXT_DIM);
            }
        }
    }

    private int empty(Canvas g, int y, String key) {
        Draw.textCentered(g, font, I18n.get(key), viewX + viewW / 2, y + 40, Theme.TEXT_MUTED);
        return y + 90;
    }

    /** Tiles, graphs (live session only), the catch, the top lists and the remaining counters. */
    private int overview(Canvas g, int y, Counters counters, boolean live, int mouseX, int mouseY) {
        if (counters.isEmpty()) {
            return empty(g, y, "multiclicker.gui.stats.nothing_yet");
        }
        y = tiles(g, y, counters, live, mouseX, mouseY);
        if (live) {
            int w = (viewW - GAP) / 2;
            cpsGraph(g, viewX, y, w, mouseX, mouseY);
            minuteGraph(g, viewX + w + GAP, y, viewW - w - GAP, mouseX, mouseY);
            y += GRAPH_HEIGHT + GAP;
        }
        y = loot(g, y, counters, mouseX, mouseY);
        y = tops(g, y, counters, mouseX, mouseY);
        return more(g, y, counters, live);
    }

    private int tiles(Canvas g, int y, Counters counters, boolean live, int mouseX, int mouseY) {
        int columns = viewW >= 360 ? 4 : 2;
        int w = (viewW - GAP * (columns - 1)) / columns;
        for (int i = 0; i < TILES.size(); i++) {
            Stat stat = TILES.get(i);
            int x = viewX + (i % columns) * (w + GAP);
            int tileY = y + (i / columns) * (TILE_HEIGHT + GAP);
            Draw.box(g, x, tileY, w, TILE_HEIGHT, 4, Theme.CARD, Theme.CARD_BORDER);
            String sub = tileNote(counters, stat, live);
            int subW = sub.isEmpty() ? 0 : font.width(sub) + 6;
            Draw.text(g, font, Draw.ellipsize(font, I18n.get(stat.translationKey()), w - 16 - subW), x + 8, tileY + 6, Theme.TEXT_DIM);
            if (!sub.isEmpty()) {
                Draw.textRight(g, font, sub, x + w - 8, tileY + 6, Theme.TEXT_MUTED);
            }
            boolean zero = counters.get(stat) == 0 && !(stat == Stat.DAMAGE && counters.get(Stat.DAMAGE_HITS) > 0);
            Gfx.push(g, x + 8, tileY + 18, 1.5F);
            g.text(font, StatFormat.value(counters, stat), 0, 0, zero ? Theme.TEXT_MUTED : Theme.TEXT);
            Gfx.pop(g);
            if (mouseX >= x && mouseX < x + w && mouseY >= tileY && mouseY < tileY + TILE_HEIGHT) {
                tooltip = tileTooltip(counters, stat);
            }
        }
        int rows = (TILES.size() + columns - 1) / columns;
        return y + rows * (TILE_HEIGHT + GAP);
    }

    private String tileNote(Counters counters, Stat stat, boolean live) {
        return switch (stat) {
            case ACTIVE_TIME -> live
                    ? I18n.get("multiclicker.gui.stats.since", TIME.format(Instant.ofEpochMilli(stats.startedAt()).atZone(ZoneId.systemDefault())))
                    : I18n.get("multiclicker.gui.stats.sessions_count", counters.get(Stat.SESSIONS));
            case CLICKS -> {
                long time = counters.get(Stat.ACTIVE_TIME);
                yield time < 1000 ? "" : I18n.get("multiclicker.gui.stats.average_cps",
                        String.format(Locale.ROOT, "%.1f", counters.get(Stat.CLICKS) * 1000.0 / time));
            }
            case DAMAGE -> {
                double dps = Statistics.dps(counters);
                yield dps < 0 || !Statistics.damageKnown(counters) ? ""
                        : I18n.get("multiclicker.gui.stats.dps", String.format(Locale.ROOT, "%.1f", dps));
            }
            default -> StatFormat.perHour(counters, stat);
        };
    }

    @Nullable
    private Component tileTooltip(Counters counters, Stat stat) {
        if (stat == Stat.DAMAGE && !Statistics.damageKnown(counters)) {
            return Component.translatable("multiclicker.gui.stats.damage_unknown");
        }
        return stat == Stat.DAMAGE ? Component.translatable(stat.translationKey() + ".desc") : null;
    }

    private void cpsGraph(Canvas g, int x, int y, int w, int mouseX, int mouseY) {
        Draw.box(g, x, y, w, GRAPH_HEIGHT, 4, Theme.CARD, Theme.CARD_BORDER);
        Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.stats.cps_graph"), w - 90), x + 8, y + 6, Theme.TEXT_DIM);
        int accent = Theme.accent();
        String target = I18n.get("multiclicker.gui.stats.target");
        String actual = I18n.get("multiclicker.gui.stats.actual");
        int legendX = x + w - 8 - font.width(target);
        Draw.text(g, font, target, legendX, y + 6, Theme.TEXT_MUTED);
        g.fill(legendX - 9, y + 9, legendX - 3, y + 11, Theme.WARNING);
        legendX -= 14 + font.width(actual);
        Draw.text(g, font, actual, legendX, y + 6, Theme.TEXT_MUTED);
        g.fill(legendX - 9, y + 7, legendX - 3, y + 13, accent);

        Timeline timeline = stats.timeline();
        int plotX = x + 8;
        int plotY = y + 22;
        int plotW = w - 16;
        int plotH = GRAPH_HEIGHT - 32;
        g.fill(plotX, plotY + plotH, plotX + plotW, plotY + plotH + 1, Theme.DIVIDER);
        int seconds = timeline.seconds();
        if (seconds == 0) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.stats.no_data"), plotX + plotW / 2, plotY + plotH / 2 - 4, Theme.TEXT_MUTED);
            return;
        }
        float max = 1;
        for (int i = 0; i < seconds; i++) {
            max = Math.max(max, Math.max(timeline.clicksAt(i), timeline.targetAt(i)));
        }
        max = (float) Math.ceil(max);
        Draw.text(g, font, StatFormat.compact(max), plotX + 1, plotY - 1, Theme.TEXT_MUTED);
        float barW = plotW / (float) Timeline.SECONDS;
        int hovered = -1;
        for (int i = 0; i < seconds; i++) {
            int left = plotX + Math.round(plotW - (seconds - i) * barW);
            int right = plotX + Math.round(plotW - (seconds - i - 1) * barW);
            int barH = Math.round(timeline.clicksAt(i) * plotH / max);
            boolean over = mouseX >= left && mouseX < right && mouseY >= plotY && mouseY < plotY + plotH;
            if (over) {
                hovered = i;
                g.fill(left, plotY, right, plotY + plotH, Theme.alpha(0xFFFFFFFF, 0.05F));
            }
            g.fill(left, plotY + plotH - barH, Math.max(left + 1, right - 1), plotY + plotH, Theme.alpha(accent, over ? 1 : 0.7F));
            float goal = timeline.targetAt(i);
            if (goal >= 0) {
                int goalY = plotY + plotH - Math.round(goal * plotH / max);
                g.fill(left, goalY, right, goalY + 1, Theme.WARNING);
            }
        }
        if (hovered >= 0) {
            float goal = timeline.targetAt(hovered);
            tooltip = Component.translatable("multiclicker.gui.stats.cps_point", seconds - hovered, timeline.clicksAt(hovered),
                    goal < 0 ? "—" : String.format(Locale.ROOT, "%.1f", goal));
        }
    }

    private void minuteGraph(Canvas g, int x, int y, int w, int mouseX, int mouseY) {
        Stat metric = Timeline.METRICS.get(Math.floorMod(lastMetric, Timeline.METRICS.size()));
        Draw.box(g, x, y, w, GRAPH_HEIGHT, 4, Theme.CARD, Theme.CARD_BORDER);
        String chip = I18n.get(metric.translationKey()) + " ▾";
        int chipW = font.width(chip) + 12;
        int chipX = x + w - 6 - chipW;
        boolean overChip = mouseX >= chipX && mouseX < chipX + chipW && mouseY >= y + 4 && mouseY < y + 17;
        Draw.rect(g, chipX, y + 4, chipW, 13, 3, overChip ? Theme.CONTROL_HOVER : Theme.CONTROL);
        Draw.textCentered(g, font, chip, chipX + chipW / 2, y + 6, Theme.TEXT);
        hits.add(new Hit(chipX, y + 4, chipW, 13, true, () -> lastMetric = (lastMetric + 1) % Timeline.METRICS.size()));
        if (overChip) {
            tooltip = Component.translatable("multiclicker.gui.stats.switch_metric");
        }
        Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.stats.per_minute"), chipX - x - 12), x + 8, y + 6, Theme.TEXT_DIM);

        Timeline timeline = stats.timeline();
        int plotX = x + 8;
        int plotY = y + 22;
        int plotW = w - 16;
        int plotH = GRAPH_HEIGHT - 32;
        g.fill(plotX, plotY + plotH, plotX + plotW, plotY + plotH + 1, Theme.DIVIDER);
        int minutes = timeline.minutes();
        if (minutes == 0) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.stats.no_data"), plotX + plotW / 2, plotY + plotH / 2 - 4, Theme.TEXT_MUTED);
            return;
        }
        // At least 3 pixels a bar; older minutes scroll off to the left.
        int visible = Math.min(minutes, plotW / 3);
        int first = minutes - visible;
        int barW = Math.min(14, plotW / Math.max(visible, 10));
        double max = 1;
        for (int i = first; i < minutes; i++) {
            max = Math.max(max, minuteValue(timeline, metric, i));
        }
        Draw.text(g, font, StatFormat.compact(Math.ceil(max)), plotX + 1, plotY - 1, Theme.TEXT_MUTED);
        int accent = Theme.accent();
        for (int i = first; i < minutes; i++) {
            int left = plotX + (i - first) * barW;
            double value = minuteValue(timeline, metric, i);
            int barH = (int) Math.round(value * plotH / max);
            boolean current = i == minutes - 1 && stats.isRunning();
            boolean over = mouseX >= left && mouseX < left + barW && mouseY >= plotY && mouseY < plotY + plotH;
            if (over) {
                g.fill(left, plotY, left + barW, plotY + plotH, Theme.alpha(0xFFFFFFFF, 0.05F));
                tooltip = Component.translatable("multiclicker.gui.stats.minute_point", timeline.firstMinute() + i + 1,
                        StatFormat.compact(Math.round(value * 10) / 10.0), Component.translatable(metric.translationKey()));
            }
            g.fill(left, plotY + plotH - barH, left + Math.max(1, barW - 1), plotY + plotH,
                    Theme.alpha(accent, over ? 1 : current ? 0.4F : 0.7F));
        }
    }

    private static double minuteValue(Timeline timeline, Stat metric, int index) {
        long value = timeline.valueAt(metric, index);
        return metric == Stat.DAMAGE ? value / 10.0 : value;
    }

    /** The catch by kind, as a bar split in four. */
    private int loot(Canvas g, int y, Counters counters, int mouseX, int mouseY) {
        long total = 0;
        for (LootKind kind : LootKind.values()) {
            total += counters.get(kind.stat());
        }
        if (total == 0) {
            return y;
        }
        int h = 40;
        Draw.box(g, viewX, y, viewW, h, 4, Theme.CARD, Theme.CARD_BORDER);
        Draw.text(g, font, I18n.get("multiclicker.gui.stats.catch"), viewX + 8, y + 6, Theme.TEXT_DIM);
        int barX = viewX + 8;
        int barW = viewW - 16;
        int barY = y + 19;
        int x = barX;
        int legendX = viewX + viewW - 8;
        LootKind[] kinds = LootKind.values();
        for (int i = kinds.length - 1; i >= 0; i--) {
            long amount = counters.get(kinds[i].stat());
            String text = I18n.get(kinds[i].stat().translationKey()) + " " + StatFormat.compact(amount);
            legendX -= font.width(text);
            Draw.text(g, font, text, legendX, y + 6, amount > 0 ? Theme.TEXT_DIM : Theme.TEXT_MUTED);
            g.fill(legendX - 8, y + 7, legendX - 3, y + 12, LOOT_COLORS[i]);
            legendX -= 16;
        }
        Draw.rect(g, barX, barY, barW, 8, 2, Theme.CONTROL);
        for (int i = 0; i < kinds.length; i++) {
            long amount = counters.get(kinds[i].stat());
            int segment = i == kinds.length - 1 ? barX + barW - x : (int) Math.round(barW * (double) amount / total);
            if (amount > 0 && segment > 0) {
                g.fill(x, barY, x + segment, barY + 8, LOOT_COLORS[i]);
                if (mouseX >= x && mouseX < x + segment && mouseY >= barY && mouseY < barY + 8) {
                    tooltip = Component.literal(I18n.get(kinds[i].stat().translationKey()) + ": " + amount
                            + String.format(Locale.ROOT, " (%.0f%%)", 100.0 * amount / total));
                }
            }
            x += segment;
        }
        return y + h + GAP;
    }

    /** Most killed mobs, most caught items and most mined blocks, side by side. */
    private int tops(Canvas g, int y, Counters counters, int mouseX, int mouseY) {
        List<Counters.Group> groups = new ArrayList<>();
        for (Counters.Group group : Counters.Group.values()) {
            if (counters.size(group) > 0) {
                groups.add(group);
            }
        }
        if (groups.isEmpty()) {
            return y;
        }
        int columns = viewW >= 360 ? groups.size() : 1;
        int w = (viewW - GAP * (columns - 1)) / columns;
        int h = 22 + TOP_ENTRIES * TOP_ROW + 2;
        for (int i = 0; i < groups.size(); i++) {
            Counters.Group group = groups.get(i);
            int x = viewX + (i % columns) * (w + GAP);
            int cardY = y + (i / columns) * (h + GAP);
            Draw.box(g, x, cardY, w, h, 4, Theme.CARD, Theme.CARD_BORDER);
            Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.stats.top." + group.key()), w - 16), x + 8, cardY + 6, Theme.TEXT_DIM);
            List<Map.Entry<String, Long>> entries = counters.top(group, TOP_ENTRIES);
            long max = entries.get(0).getValue();
            int rowY = cardY + 20;
            for (Map.Entry<String, Long> entry : entries) {
                int textX = x + 8;
                ItemStack icon = icon(group, entry.getKey());
                if (!icon.isEmpty()) {
                    g.item(icon, x + 6, rowY);
                    textX = x + 26;
                } else {
                    Draw.rect(g, x + 10, rowY + 6, 5, 5, 2, Theme.alpha(Theme.accent(), 0.8F));
                    textX = x + 20;
                }
                String count = StatFormat.compact(entry.getValue());
                int countW = font.width(count);
                Draw.textRight(g, font, count, x + w - 8, rowY + 4, Theme.TEXT);
                String name = Draw.ellipsize(font, name(group, entry.getKey()).getString(), x + w - 14 - countW - textX);
                Draw.text(g, font, name, textX, rowY + 2, Theme.TEXT_DIM);
                int barW = (int) Math.round((x + w - 8 - textX) * (double) entry.getValue() / max);
                g.fill(textX, rowY + 12, textX + Math.max(1, barW), rowY + 13, Theme.alpha(Theme.accent(), 0.5F));
                if (mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + TOP_ROW) {
                    tooltip = Component.literal(entry.getKey());
                }
                rowY += TOP_ROW;
            }
        }
        int rows = (groups.size() + columns - 1) / columns;
        return y + rows * (h + GAP);
    }

    /** The other counters, two columns of name and value. */
    private int more(Canvas g, int y, Counters counters, boolean live) {
        List<Stat> shown = new ArrayList<>();
        for (Stat stat : MORE) {
            if (!(live && stat == Stat.SESSIONS)) {
                shown.add(stat);
            }
        }
        int columns = viewW >= 360 ? 2 : 1;
        int rows = (shown.size() + columns - 1) / columns;
        int h = 8 + rows * 12 + 4;
        Draw.box(g, viewX, y, viewW, h, 4, Theme.CARD, Theme.CARD_BORDER);
        int w = (viewW - 16 - 16 * (columns - 1)) / columns;
        for (int i = 0; i < shown.size(); i++) {
            Stat stat = shown.get(i);
            int x = viewX + 8 + (i / rows) * (w + 16);
            int rowY = y + 7 + (i % rows) * 12;
            String value = StatFormat.value(counters, stat);
            String rate = stat == Stat.COMBAT_TIME || stat == Stat.SESSIONS ? "" : StatFormat.perHour(counters, stat);
            String right = rate.isEmpty() || counters.get(stat) == 0 ? value : value + "  " + rate;
            Draw.textRight(g, font, right, x + w, rowY, counters.get(stat) == 0 ? Theme.TEXT_MUTED : Theme.TEXT);
            Draw.text(g, font, Draw.ellipsize(font, I18n.get(stat.translationKey()), w - font.width(right) - 8), x, rowY, Theme.TEXT_DIM);
        }
        return y + h + GAP;
    }

    private int history(Canvas g, int y, int mouseX, int mouseY) {
        List<SessionRecord> history = stats.store().history();
        if (history.isEmpty()) {
            return empty(g, y, "multiclicker.gui.stats.no_history");
        }
        for (SessionRecord record : history) {
            boolean over = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= y && mouseY < y + HISTORY_ROW - 2;
            Draw.box(g, viewX, y, viewW, HISTORY_ROW - 2, 4, over ? Theme.CONTROL : Theme.CARD, Theme.CARD_BORDER);
            String when = DATE.format(Instant.ofEpochMilli(record.start()).atZone(ZoneId.systemDefault()));
            String duration = Statistics.formatDuration(record.duration());
            Draw.text(g, font, when, viewX + 8, y + 4, Theme.TEXT);
            Draw.textRight(g, font, duration, viewX + viewW - 8, y + 4, Theme.TEXT_DIM);
            int serverX = viewX + 16 + font.width(when);
            Draw.text(g, font, Draw.ellipsize(font, record.server().isEmpty() ? "" : serverName(record.server()),
                    viewX + viewW - 16 - font.width(duration) - serverX), serverX, y + 4, Theme.TEXT_MUTED);
            Draw.text(g, font, Draw.ellipsize(font, summary(record.counters()), viewW - 16), viewX + 8, y + 15, Theme.TEXT_MUTED);
            hits.add(new Hit(viewX, y, viewW, HISTORY_ROW - 2, true, () -> {
                openedRecord = record;
                scroll = 0;
            }));
            y += HISTORY_ROW;
        }
        return y;
    }

    private String summary(Counters counters) {
        StringBuilder text = new StringBuilder();
        for (Stat stat : List.of(Stat.CLICKS, Stat.KILLS, Stat.DAMAGE, Stat.CATCHES, Stat.CROPS, Stat.BLOCKS, Stat.XP)) {
            if (counters.get(stat) > 0) {
                if (text.length() > 0) {
                    text.append("  ·  ");
                }
                text.append(I18n.get(stat.translationKey())).append(' ').append(StatFormat.value(counters, stat));
            }
        }
        return text.length() == 0 ? I18n.get("multiclicker.gui.stats.nothing_yet") : text.toString();
    }

    /** One session from the history: a line to go back, then the same overview as the other tabs. */
    private int record(Canvas g, int y, SessionRecord record, int mouseX, int mouseY) {
        String back = "◀ " + I18n.get("multiclicker.gui.stats.tab.history");
        int backW = font.width(back) + 12;
        boolean over = mouseX >= viewX && mouseX < viewX + backW && mouseY >= y && mouseY < y + 14;
        Draw.rect(g, viewX, y, backW, 14, 3, over ? Theme.CONTROL_HOVER : Theme.CONTROL);
        Draw.text(g, font, back, viewX + 6, y + 3, Theme.TEXT);
        hits.add(new Hit(viewX, y, backW, 14, true, () -> {
            openedRecord = null;
            scroll = 0;
        }));
        String when = DATE.format(Instant.ofEpochMilli(record.start()).atZone(ZoneId.systemDefault()))
                + (record.server().isEmpty() ? "" : "  ·  " + serverName(record.server()));
        Draw.text(g, font, Draw.ellipsize(font, when, viewW - backW - 8), viewX + backW + 8, y + 3, Theme.TEXT_DIM);
        return overview(g, y + 14 + GAP, record.counters(), false, mouseX, mouseY);
    }

    private void drawScrollbar(Canvas g) {
        int maxScroll = contentHeight - viewH;
        if (maxScroll <= 0) {
            return;
        }
        int barH = Math.max(16, viewH * viewH / contentHeight);
        int barY = viewY + Math.round((viewH - barH) * scroll / maxScroll);
        g.fill(viewX + viewW + 3, barY, viewX + viewW + 5, barY + barH, Theme.alpha(0xFFFFFFFF, 0.25F));
    }

    // --- Names and icons ------------------------------------------------------------------------

    private static String serverName(String address) {
        return address.equals(ConfigManager.SINGLEPLAYER) ? I18n.get("multiclicker.gui.stats.singleplayer") : address;
    }

    private static Component name(Counters.Group group, String id) {
        return switch (group) {
            case MOBS -> Component.translatable("entity." + id.replace(':', '.'));
            case LOOT -> {
                ItemStack stack = new ItemStack(Ids.item(id));
                yield stack.is(Items.AIR) ? Component.literal(id) : stack.getHoverName();
            }
            case BLOCKS -> {
                Block block = Ids.block(id);
                yield block == Blocks.AIR ? Component.literal(id) : block.getName();
            }
        };
    }

    private ItemStack icon(Counters.Group group, String id) {
        if (group == Counters.Group.MOBS) {
            return ItemStack.EMPTY;
        }
        return icons.computeIfAbsent(group.key() + "/" + id, key -> group == Counters.Group.LOOT
                ? new ItemStack(Ids.item(id)) : new ItemStack(Ids.block(id).asItem()));
    }

    // --- Input ----------------------------------------------------------------------------------

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean inView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
            for (Hit hit : List.copyOf(hits)) {
                if (hit.contains(mouseX, mouseY) && (!hit.inView() || inView)) {
                    minecraft.getSoundManager().play(Sounds.click(1.0F, 0.35F));
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.clicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= viewX && mouseX < viewX + viewW + 6 && mouseY >= viewY && mouseY < viewY + viewH) {
            scroll -= (float) scrollY * 24;
            return true;
        }
        return super.scrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
