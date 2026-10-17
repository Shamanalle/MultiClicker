package io.github.shamanalle.multiclicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Gfx;
import io.github.shamanalle.multiclicker.compat.Keys;
import io.github.shamanalle.multiclicker.compat.ModScreen;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Widgets;
import io.github.shamanalle.multiclicker.compat.Sounds;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.gui.widget.SettingControl;
import io.github.shamanalle.multiclicker.gui.widget.SliderControl;
import io.github.shamanalle.multiclicker.gui.widget.ToggleControl;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.module.session.GoalsModule;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.KeySetting;
import io.github.shamanalle.multiclicker.setting.Setting;
import io.github.shamanalle.multiclicker.stats.Counters;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;
import io.github.shamanalle.multiclicker.stats.Statistics;
import io.github.shamanalle.multiclicker.util.KeyConflicts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main settings menu: an overview page and the categories on the left, one card per module on
 * the right, a search field and the master on/off switch in the header.
 */
public class ConfigScreen extends ModScreen {
    private static final int HEADER = 30;
    private static final int FOOTER = 16;
    private static final int SIDEBAR_ITEM = 20;
    private static final int COMPACT_SIDEBAR = 28;
    private static final int CARD_HEADER = 32;
    private static final int DESCRIPTION_LINE = 10;
    private static final int ROW = 18;
    private static final int SUBHEADER = 17;
    private static final int ACTION_ROW = 22;
    private static final int CARD_GAP = 6;
    private static final int CARD_MARGIN = 8;
    private static final long TOOLTIP_DELAY_MS = 350;
    private static final String[] CLICKER_GROUPS = {"attack", "use", "jump", "general"};
    private static final String[] HINTS = {"multiclicker.gui.hint", "multiclicker.gui.hint.value",
            "multiclicker.gui.hint.keys", "multiclicker.gui.hint.nav", "multiclicker.gui.hint.search"};
    private static final long HINT_MS = 6000;
    private static final int CHIP_HEIGHT = 14;
    private static final long FLASH_MS = 900;
    /** The counters on the overview page. */
    private static final Stat[] OVERVIEW_STATS = {Stat.ATTACKS, Stat.KILLS, Stat.CATCHES, Stat.CROPS, Stat.BLOCKS};

    /** The page shown last, kept while the game runs; {@code null} is the overview. */
    @Nullable
    private static Category lastCategory;
    /** Cards the player opened or closed by hand, by module id; the others are open while their module is on. */
    private static final Map<String, Boolean> EXPANDED = new HashMap<>();

    @Nullable
    private final Screen parent;
    private final MultiClicker mod = MultiClicker.get();
    private final List<Card> cards = new ArrayList<>();
    private final Map<Setting<?>, SettingControl> controls = new HashMap<>();
    private final Map<Category, Anim> categoryHover = new EnumMap<>(Category.class);
    private final Anim overviewHover = new Anim(0);
    private final Anim profilesHover = new Anim(0);
    private final Anim statsHover = new Anim(0);
    private final Anim masterAnim;
    private final Map<Object, Anim> headerHover = new HashMap<>();
    /** Slides the cards in when another category is shown. */
    private final Anim contentShift = new Anim(0);
    private final KeyCapture capture = new KeyCapture();
    private final long openedAt = System.currentTimeMillis();
    /** The clickable parts of the overview page as drawn last frame. */
    private final List<Hit> hits = new ArrayList<>();
    /** The setting rows in the order they were drawn last frame, for the arrow keys. */
    private final List<Row> navigation = new ArrayList<>();
    /** The slider whose number is being typed, if any. */
    @Nullable
    private SliderControl editing;

    @Nullable
    private Category category = lastCategory;
    private String query = "";
    private EditBox search;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarW;
    private int sidebarItem = SIDEBAR_ITEM;
    private boolean compactSidebar;
    private boolean showVersion;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private int searchX;
    private int searchW;
    private int masterX;
    private int masterW;

    private float scroll;
    private float scrollTarget;
    private int contentHeight;
    private boolean draggingScrollbar;
    @Nullable
    private SettingControl dragging;
    private long lastFrameNanos;
    @Nullable
    private Object hoverKey;
    private long hoverSince;
    /** Whether the hovered setting's name is cut off, so the tooltip has to repeat it in full. */
    private boolean hoveredNameCut;
    /** The row picked with the arrow keys; moving the mouse drops it. */
    @Nullable
    private Setting<?> focused;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    /** A card to scroll to once it is laid out, and to light up briefly. */
    @Nullable
    private Module scrollTo;
    @Nullable
    private Module flash;
    private long flashSince;

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("multiclicker.gui.title"));
        this.parent = parent;
        this.masterAnim = new Anim(mod.isActive() ? 1 : 0);
    }

    /** Shows a category and clears the search. */
    public void showCategory(@Nullable Category target) {
        category = lastCategory = target;
        query = "";
        focused = null;
        if (search != null) {
            search.setValue("");
        }
        rebuild();
        contentShift.set(1);
    }

    /** Shows the overview page. */
    public void showOverview() {
        showCategory(null);
    }

    /** Shows the card of a module, opened, and scrolls to it. */
    public void showModule(Module module) {
        showCategory(module.category());
        EXPANDED.put(module.id(), true);
        scrollTo = module;
    }

    /** Fills the search field, as if the player typed the text. */
    public void search(String text) {
        if (search != null) {
            search.setValue(text);
        }
    }

    private boolean isOverview() {
        return category == null && query.isEmpty();
    }

    // --- Model ----------------------------------------------------------------------------------

    private static final class Card {
        final Module module;
        final List<Row> rows;
        @Nullable
        final ToggleControl toggle;
        List<String> description = List.of();
        int descriptionWidth = -1;
        int headerHeight = CARD_HEADER;
        int y;
        /** Distance from the top of the content, for scrolling to the card. */
        int offset;
        int height;
        /** Header buttons as drawn last frame; -1 when absent. */
        int chipX = -1;
        int chipW;
        int resetX = -1;

        Card(Module module, List<Row> rows, @Nullable ToggleControl toggle) {
            this.module = module;
            this.rows = rows;
            this.toggle = toggle;
        }
    }

    private static final class Row {
        @Nullable
        final Setting<?> setting;
        @Nullable
        final Component subheader;
        @Nullable
        final FlatButton button;
        int y;
        boolean visible;

        Row(@Nullable Setting<?> setting, @Nullable Component subheader, @Nullable FlatButton button) {
            this.setting = setting;
            this.subheader = subheader;
            this.button = button;
        }

        int height() {
            return subheader != null ? SUBHEADER : button != null ? ACTION_ROW : ROW;
        }
    }

    /** A clickable part of the overview page. */
    private record Hit(Object key, int x, int y, int w, int h, @Nullable Runnable left, @Nullable Runnable right,
                       @Nullable Component tooltip) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }

    private SettingControl control(Setting<?> setting) {
        return controls.computeIfAbsent(setting, s -> SettingControl.of(s, this, capture));
    }

    private void rebuild() {
        finishEditing(true);
        capture.cancel();
        cards.clear();
        scroll = scrollTarget = 0;
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Module module : mod.modules()) {
            List<Row> rows = new ArrayList<>();
            if (q.isEmpty()) {
                if (module.category() != category) {
                    continue;
                }
                addRows(module, rows, null);
            } else {
                boolean moduleMatches = matches(module.name(), q) || matches(module.description(), q)
                        || matchesId(module.id(), q);
                addRows(module, rows, moduleMatches ? null : q);
                if (rows.isEmpty() && !moduleMatches) {
                    continue;
                }
            }
            BoolSetting enabled = module.enabledSetting();
            cards.add(new Card(module, rows, enabled != null ? (ToggleControl) control(enabled) : null));
        }
    }

    private void addRows(Module module, List<Row> rows, @Nullable String filter) {
        List<Setting<?>> groupStarts = module instanceof ClickerModule clicker ? clicker.groupStarts() : List.of();
        for (Setting<?> setting : module.settings()) {
            if (setting == module.enabledSetting() || setting == module.keySetting() || setting.isInternal()) {
                continue;
            }
            if (filter != null) {
                if (matches(setting.name(), filter) || setting.description() != null && matches(setting.description(), filter)
                        || matchesId(setting.key(), filter)) {
                    rows.add(new Row(setting, null, null));
                }
                continue;
            }
            int group = groupStarts.indexOf(setting);
            if (group >= 0) {
                rows.add(new Row(null, Component.translatable("multiclicker.group." + CLICKER_GROUPS[group]), null));
            }
            rows.add(new Row(setting, null, null));
        }
        for (Module.Action action : module.actions()) {
            Component label = action.label(module);
            if (filter == null || matches(label, filter)) {
                rows.add(new Row(null, null, new FlatButton(label, FlatButton.Style.NORMAL, () -> action.run().accept(this))));
            }
        }
    }

    private static boolean matches(Component text, String query) {
        return text.getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** The English ids ("auto_eat", "attack_interval") also match, whatever the game language. */
    private static boolean matchesId(String id, String query) {
        return id.replace('_', ' ').contains(query) || id.contains(query);
    }

    /** Whether the settings of a card are shown: always while searching, else as the player left it or while the module is on. */
    private boolean isExpanded(Card card) {
        if (!query.isEmpty() || !hasBody(card)) {
            return true;
        }
        Boolean explicit = EXPANDED.get(card.module.id());
        return explicit != null ? explicit : card.module.isEnabled();
    }

    private static boolean hasBody(Card card) {
        return !card.rows.isEmpty();
    }

    // --- Setup ----------------------------------------------------------------------------------

    @Override
    protected void init() {
        panelW = Math.min(width - 24, 640);
        panelH = Math.min(height - 24, 400);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        // The sidebar fits its longest label; on small screens it shrinks to icons with tooltips.
        int widestLabel = Math.max(font.width(I18n.get("multiclicker.gui.profiles")),
                font.width(I18n.get("multiclicker.gui.overview")));
        for (Category entry : Category.values()) {
            widestLabel = Math.max(widestLabel, font.width(entry.title().getString()));
        }
        int fullSidebar = Mth.clamp(widestLabel + 46, 96, 150);
        compactSidebar = panelW - fullSidebar < 260;
        sidebarW = compactSidebar ? COMPACT_SIDEBAR : fullSidebar;
        // The overview, the categories and the two links share the height of the sidebar.
        int items = Category.values().length + 3;
        sidebarItem = Mth.clamp((panelH - HEADER - 22) / items, 14, SIDEBAR_ITEM);
        viewX = panelX + sidebarW;
        viewY = panelY + HEADER;
        viewW = panelW - sidebarW;
        viewH = panelH - HEADER - FOOTER;

        masterW = Math.max(font.width(I18n.get("multiclicker.gui.active")), font.width(I18n.get("multiclicker.gui.inactive"))) + 24;
        masterX = panelX + panelW - masterW - 8;
        int titleEnd = panelX + 15 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 12;
        showVersion = masterX - 8 - (titleEnd + font.width("v" + mod.version()) + 5) >= 110;
        if (showVersion) {
            titleEnd += font.width("v" + mod.version()) + 5;
        }
        searchW = Mth.clamp(masterX - 8 - titleEnd, 60, 170);
        searchX = masterX - 8 - searchW;

        search = new EditBox(font, searchX + 6, panelY + 11, searchW - 12, 10, Component.translatable("multiclicker.gui.search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setTextColor(Theme.TEXT);
        Widgets.hint(search, Component.translatable("multiclicker.gui.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(value -> {
            if (!value.equals(query)) {
                query = value;
                focused = null;
                rebuild();
            }
        });
        addRenderableWidget(search);
        rebuild();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        finishEditing(true);
        mod.saveConfig();
        Screens.open(minecraft, parent);
    }

    // --- Rendering ------------------------------------------------------------------------------

    @Override
    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0 ? 0 : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = now;
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            if (lastMouseX >= 0) {
                focused = null;
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        KeyConflicts.refresh(minecraft);

        Draw.box(g, panelX, panelY, panelW, panelH, 4, Theme.PANEL, Theme.CARD_BORDER);
        Draw.rect(g, panelX + 1, panelY + HEADER, sidebarW - 1, panelH - HEADER - 1, 3, Theme.SIDEBAR);
        g.fill(panelX + 1, panelY + HEADER, panelX + sidebarW + 1, panelY + HEADER + 4, Theme.SIDEBAR);
        g.fill(panelX + sidebarW, panelY + HEADER, panelX + sidebarW + 1, panelY + panelH - 1, Theme.DIVIDER);
        g.fill(panelX + 1, panelY + HEADER - 1, panelX + panelW - 1, panelY + HEADER, Theme.DIVIDER);

        Object hovered = null;
        renderHeader(g, mouseX, mouseY, delta);
        hovered = renderSidebar(g, mouseX, mouseY, delta, hovered);
        hovered = renderContent(g, mouseX, mouseY, delta, hovered);
        renderFooter(g);

        super.draw(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY, hovered);
    }

    private void renderHeader(Canvas g, int mouseX, int mouseY, float delta) {
        int textY = panelY + 11;
        int accent = Theme.accent();
        Draw.rect(g, panelX + 8, panelY + 10, 3, 10, 1, accent);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), panelX + 15, textY, Theme.TEXT);
        if (showVersion) {
            Draw.text(g, font, "v" + mod.version(), panelX + 15 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 5,
                    textY, Theme.TEXT_MUTED);
        }

        boolean searchFocused = search.isFocused();
        Draw.box(g, searchX, panelY + 7, searchW, 16, 3, Theme.CONTROL,
                searchFocused ? Theme.alpha(accent, 0.8F) : Theme.CONTROL);
        if (search.getValue().isEmpty() && !searchFocused) {
            Draw.textRight(g, font, "⌕", searchX + searchW - 6, textY, Theme.TEXT_MUTED);
        }

        boolean inWorld = minecraft.player != null;
        float on = masterAnim.update(mod.isActive() ? 1 : 0, delta, 14);
        boolean over = inWorld && isOverMaster(mouseX, mouseY);
        int base = Theme.mix(Theme.CONTROL, Theme.alpha(Theme.SUCCESS, 0.22F), on);
        Draw.rect(g, masterX, panelY + 7, masterW, 16, 8, over ? Theme.mix(base, 0xFFFFFFFF, 0.08F) : base);
        int dotColor = inWorld ? Theme.mix(Theme.TEXT_MUTED, Theme.SUCCESS, on) : Theme.alpha(Theme.TEXT_MUTED, 0.5F);
        Draw.rect(g, masterX + 8, panelY + 12, 6, 6, 3, dotColor);
        String label = I18n.get(mod.isActive() ? "multiclicker.gui.active" : "multiclicker.gui.inactive");
        Draw.text(g, font, label, masterX + 18, textY, inWorld ? (mod.isActive() ? Theme.SUCCESS : Theme.TEXT_DIM) : Theme.TEXT_MUTED);
    }

    private boolean isOverMaster(double mouseX, double mouseY) {
        return mouseX >= masterX && mouseX < masterX + masterW && mouseY >= panelY + 7 && mouseY < panelY + 23;
    }

    private int sidebarTop() {
        return panelY + HEADER + 6;
    }

    /** The first category item, below the overview. */
    private int categoriesTop() {
        return sidebarTop() + sidebarItem + 4;
    }

    private int profilesItemY() {
        return panelY + panelH - sidebarItem - 6;
    }

    private int statsItemY() {
        return profilesItemY() - sidebarItem;
    }

    /** The y of the text in a sidebar item. */
    private int itemTextY(int itemY) {
        return itemY + (sidebarItem - 10) / 2;
    }

    private Object renderSidebar(Canvas g, int mouseX, int mouseY, float delta, Object hovered) {
        int accent = Theme.accent();
        int conflicts = KeyConflicts.all().size();
        String conflictText = conflicts > 0 ? "⚠" + conflicts : "";
        if (renderSidebarItem(g, mouseX, mouseY, delta, sidebarTop(), overviewHover, isOverview(), "⌂",
                I18n.get("multiclicker.gui.overview"), conflictText, Theme.WARNING)) {
            hovered = "overview";
        }
        g.fill(panelX + 8, categoriesTop() - 3, panelX + sidebarW - 8, categoriesTop() - 2, Theme.DIVIDER);

        int y = categoriesTop();
        for (Category entry : Category.values()) {
            boolean selected = query.isEmpty() && entry == category;
            int count = enabledCount(entry);
            Anim hover = categoryHover.computeIfAbsent(entry, c -> new Anim(0));
            if (renderSidebarItem(g, mouseX, mouseY, delta, y, hover, selected, entry.icon(), entry.title().getString(),
                    count > 0 ? Integer.toString(count) : "", Theme.alpha(accent, selected ? 1 : 0.7F))) {
                hovered = entry;
            }
            y += sidebarItem;
        }

        int statsY = statsItemY();
        g.fill(panelX + 8, statsY - 3, panelX + sidebarW - 8, statsY - 2, Theme.DIVIDER);
        if (renderSidebarItem(g, mouseX, mouseY, delta, statsY, statsHover, false, "▤", I18n.get("multiclicker.gui.stats"),
                "", 0)) {
            hovered = "stats";
        }
        if (renderSidebarItem(g, mouseX, mouseY, delta, profilesItemY(), profilesHover, false, "☰",
                I18n.get("multiclicker.gui.profiles"), "", 0)) {
            hovered = "profiles";
        }
        return hovered;
    }

    /** One item of the sidebar: an icon, a label and a count on the right; returns whether it is hovered. */
    private boolean renderSidebarItem(Canvas g, int mouseX, int mouseY, float delta, int y, Anim hover, boolean selected,
                                      String icon, String label, String count, int countColor) {
        int accent = Theme.accent();
        boolean over = isOverSidebarItem(mouseX, mouseY, y);
        float h = hover.update(over || selected ? 1 : 0, delta, 18);
        if (h > 0) {
            int fill = selected ? Theme.alpha(accent, 0.16F) : Theme.alpha(0xFFFFFFFF, 0.05F * h);
            Draw.rect(g, panelX + 4, y, sidebarW - 8, sidebarItem - 2, 3, fill);
        }
        if (selected) {
            Draw.rect(g, panelX + 4, y + 4, 2, sidebarItem - 10, 1, accent);
        }
        int textY = itemTextY(y);
        int iconColor = selected ? accent : Theme.mix(Theme.TEXT_MUTED, accent, over ? h : 0);
        if (compactSidebar) {
            Draw.textCentered(g, font, icon, panelX + sidebarW / 2, textY, iconColor);
            if (!count.isEmpty() && countColor == Theme.WARNING) {
                Draw.rect(g, panelX + sidebarW - 9, y + 2, 4, 4, 2, Theme.WARNING);
            }
        } else {
            Draw.text(g, font, icon, panelX + 12, textY, iconColor);
            int maxLabel = sidebarW - 38 - font.width(count);
            int textColor = selected ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h);
            Draw.text(g, font, Draw.ellipsize(font, label, maxLabel), panelX + 24, textY, textColor);
            if (!count.isEmpty()) {
                Draw.textRight(g, font, count, panelX + sidebarW - 10, textY, countColor);
            }
        }
        return over;
    }

    private boolean isOverSidebarItem(double mouseX, double mouseY, int itemY) {
        return mouseX >= panelX + 4 && mouseX < panelX + sidebarW - 4 && mouseY >= itemY && mouseY < itemY + sidebarItem - 2;
    }

    private int enabledCount(Category entry) {
        int count = 0;
        for (Module module : mod.modules()) {
            if (module.category() == entry && module.enabledSetting() != null && module.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private Object renderContent(Canvas g, int mouseX, int mouseY, float delta, Object hovered) {
        scroll += (scrollTarget - scroll) * Math.min(1, delta * 16);
        if (Math.abs(scrollTarget - scroll) < 0.5F) {
            scroll = scrollTarget;
        }
        boolean mouseInView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
        int cardX = viewX + CARD_MARGIN;
        int cardW = viewW - CARD_MARGIN * 2 - 4;

        g.enableScissor(viewX + 1, viewY, viewX + viewW - 1, viewY + viewH);
        int slide = Math.round(contentShift.update(0, delta, 12) * 14);
        int top = viewY + CARD_MARGIN - Math.round(scroll) + slide;
        navigation.clear();
        hits.clear();
        if (isOverview()) {
            int bottom = renderOverview(g, cardX, top, cardW, mouseX, mouseY, mouseInView);
            contentHeight = bottom - top + CARD_MARGIN * 2;
            for (Hit hit : hits) {
                if (mouseInView && hit.contains(mouseX, mouseY)) {
                    hovered = hit;
                }
            }
        } else {
            int y = top;
            for (Card card : cards) {
                card.y = y;
                card.offset = y - top;
                layoutDescription(card, cardW);
                card.headerHeight = CARD_HEADER + (card.description.size() - 1) * DESCRIPTION_LINE;
                int height = card.headerHeight;
                boolean anyRow = false;
                boolean expanded = isExpanded(card);
                for (Row row : card.rows) {
                    row.visible = expanded && (row.setting == null || row.setting.isVisible());
                    if (row.visible) {
                        row.y = y + height;
                        height += row.height();
                        anyRow = true;
                        if (row.setting != null) {
                            navigation.add(row);
                        }
                    }
                }
                card.height = height + (anyRow ? 5 : 0);
                if (card.y + card.height > viewY && card.y < viewY + viewH) {
                    hovered = drawCard(g, card, cardX, cardW, mouseX, mouseY, mouseInView, delta, hovered);
                }
                y += card.height + CARD_GAP;
            }
            contentHeight = y - top + CARD_MARGIN * 2 - CARD_GAP;
            if (cards.isEmpty()) {
                Draw.textCentered(g, font, I18n.get("multiclicker.gui.nothing_found"), viewX + viewW / 2,
                        viewY + viewH / 2 - 4, Theme.TEXT_MUTED);
            }
        }
        g.disableScissor();

        if (scrollTo != null) {
            for (Card card : cards) {
                if (card.module == scrollTo) {
                    scrollTarget = Mth.clamp(card.offset, 0, maxScroll());
                    flash = scrollTo;
                    flashSince = System.currentTimeMillis();
                }
            }
            scrollTo = null;
        }
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll());
        if (maxScroll() > 0) {
            int trackX = viewX + viewW - 5;
            int thumbH = Math.max(20, viewH * viewH / Math.max(1, contentHeight));
            int thumbY = viewY + Math.round((viewH - thumbH) * (scroll / maxScroll()));
            boolean overBar = draggingScrollbar || mouseX >= trackX - 2 && mouseX < trackX + 5 && mouseInView;
            Draw.rect(g, trackX, thumbY + 2, 3, thumbH - 4, 1, overBar ? Theme.TEXT_MUTED : Theme.CONTROL_HOVER);
        }
        return hovered;
    }

    // --- Overview -------------------------------------------------------------------------------

    /** Draws the overview page from {@code y}; returns where it ends. */
    private int renderOverview(Canvas g, int x, int y, int w, int mouseX, int mouseY, boolean mouseInView) {
        y = drawStatusCard(g, x, y, w, mouseX, mouseY, mouseInView) + CARD_GAP;
        int boxH = 68;
        if (w >= 330) {
            int half = (w - CARD_GAP) / 2;
            drawSessionCard(g, x, y, half, boxH, mouseX, mouseY, mouseInView);
            drawGoalCard(g, x + half + CARD_GAP, y, w - half - CARD_GAP, boxH, mouseX, mouseY, mouseInView);
            y += boxH + CARD_GAP;
        } else {
            drawSessionCard(g, x, y, w, boxH, mouseX, mouseY, mouseInView);
            y += boxH + CARD_GAP;
            drawGoalCard(g, x, y, w, boxH, mouseX, mouseY, mouseInView);
            y += boxH + CARD_GAP;
        }
        y = drawModulesCard(g, x, y, w, mouseX, mouseY, mouseInView);
        y = drawConflictsCard(g, x, y + CARD_GAP, w, mouseX, mouseY, mouseInView);
        return y;
    }

    private boolean over(int mouseX, int mouseY, boolean mouseInView, int x, int y, int w, int h) {
        return mouseInView && isOver(mouseX, mouseY, x, y, w, h);
    }

    /** A card title on the overview, with an optional link on the right. */
    private void overviewTitle(Canvas g, int x, int y, int w, String text, int color, @Nullable String link,
                               @Nullable Runnable action, int mouseX, int mouseY, boolean mouseInView) {
        g.text(font, Component.literal(text).withStyle(ChatFormatting.BOLD), x + 10, y + 8, color);
        if (link != null) {
            String label = link + " ›";
            int linkW = font.width(label);
            int linkX = x + w - 10 - linkW;
            boolean over = over(mouseX, mouseY, mouseInView, linkX - 3, y + 4, linkW + 6, 16);
            Draw.text(g, font, label, linkX, y + 8, over ? Theme.accent() : Theme.TEXT_MUTED);
            hits.add(new Hit(link, linkX - 3, y + 4, linkW + 6, 16, action, null, null));
        }
    }

    private int drawStatusCard(Canvas g, int x, int y, int w, int mouseX, int mouseY, boolean mouseInView) {
        int h = 42;
        boolean active = mod.isActive();
        boolean inWorld = minecraft.player != null;
        Draw.box(g, x, y, w, h, 4, Theme.CARD, Theme.CARD_BORDER);
        Draw.rect(g, x + 1, y + 8, 2, h - 16, 1, active ? Theme.SUCCESS : Theme.CONTROL_HOVER);

        // The profile on the right, as a button that opens the profiles.
        String profile = mod.config().activeProfile();
        String profileText = "☰ " + (profile != null ? profile : I18n.get("multiclicker.gui.overview.no_profile"));
        int profileW = Math.min(w / 2 - 10, font.width(profileText) + 16);
        int profileX = x + w - 10 - profileW;
        int profileY = y + (h - 16) / 2;
        boolean overProfile = over(mouseX, mouseY, mouseInView, profileX, profileY, profileW, 16);
        Draw.rect(g, profileX, profileY, profileW, 16, 3, overProfile ? Theme.CONTROL_HOVER : Theme.CONTROL);
        Draw.textCentered(g, font, Draw.ellipsize(font, profileText, profileW - 10), profileX + profileW / 2, profileY + 4,
                profile != null ? Theme.TEXT : Theme.TEXT_DIM);
        hits.add(new Hit("profile", profileX, profileY, profileW, 16, this::openProfiles, null,
                Component.translatable("multiclicker.gui.profiles.desc")));

        int textW = profileX - x - 20;
        String state = I18n.get(active ? "multiclicker.gui.overview.on" : "multiclicker.gui.overview.off");
        g.text(font, Component.literal(Draw.ellipsize(font, state, textW)).withStyle(ChatFormatting.BOLD), x + 10, y + 9,
                active ? Theme.SUCCESS : Theme.TEXT);
        String detail;
        if (!inWorld) {
            detail = I18n.get("multiclicker.gui.overview.no_world");
        } else if (active) {
            Statistics stats = mod.stats();
            detail = I18n.get("multiclicker.gui.overview.running", Statistics.formatDuration(stats.elapsedMillis()),
                    stats.clicksPerSecond());
        } else {
            detail = I18n.get("multiclicker.gui.overview.toggle", mod.toggleKey.getTranslatedKeyMessage().getString());
        }
        Draw.text(g, font, Draw.ellipsize(font, detail, textW), x + 10, y + 23, Theme.TEXT_MUTED);
        return y + h;
    }

    private void drawSessionCard(Canvas g, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseInView) {
        Draw.box(g, x, y, w, h, 4, Theme.CARD, Theme.CARD_BORDER);
        overviewTitle(g, x, y, w, I18n.get("multiclicker.gui.overview.session"), Theme.TEXT,
                I18n.get("multiclicker.gui.overview.all_stats"), this::openStats, mouseX, mouseY, mouseInView);
        Statistics stats = mod.stats();
        Counters session = stats.session();
        int valueColor = stats.isRunning() ? Theme.TEXT : Theme.TEXT_DIM;
        // Three columns, two rows: the time, then the counters.
        int columns = 3;
        int cellW = (w - 20) / columns;
        for (int i = 0; i <= OVERVIEW_STATS.length; i++) {
            int cellX = x + 10 + (i % columns) * cellW;
            int cellY = y + 24 + (i / columns) * 21;
            String value;
            String label;
            if (i == 0) {
                value = Statistics.formatDuration(stats.elapsedMillis());
                label = I18n.get("multiclicker.hud.time");
            } else {
                Stat stat = OVERVIEW_STATS[i - 1];
                value = StatFormat.value(session, stat);
                label = I18n.get(stat.translationKey());
            }
            Draw.text(g, font, Draw.ellipsize(font, value, cellW - 4), cellX, cellY, valueColor);
            Draw.text(g, font, Draw.ellipsize(font, label, cellW - 4), cellX, cellY + 10, Theme.TEXT_MUTED);
        }
    }

    private void drawGoalCard(Canvas g, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseInView) {
        GoalsModule goals = mod.goals();
        Draw.box(g, x, y, w, h, 4, Theme.CARD, Theme.CARD_BORDER);
        overviewTitle(g, x, y, w, goals.name().getString(), Theme.TEXT, I18n.get("multiclicker.gui.overview.edit"),
                () -> showModule(goals), mouseX, mouseY, mouseInView);
        if (goals.isEnabled()) {
            float progress = goals.progress();
            Draw.text(g, font, Draw.ellipsize(font, goals.describe().getString(), w - 20), x + 10, y + 26, Theme.TEXT_DIM);
            int barW = w - 20;
            Draw.rect(g, x + 10, y + 40, barW, 4, 2, Theme.CONTROL);
            int filled = Math.round(barW * progress);
            if (filled > 0) {
                Draw.rect(g, x + 10, y + 40, Math.max(4, filled), 4, 2, progress >= 1 ? Theme.SUCCESS : Theme.accent());
            }
            Draw.text(g, font, goals.progressText(), x + 10, y + 50, Theme.TEXT);
            Draw.textRight(g, font, Math.round(progress * 100) + "%", x + w - 10, y + 50,
                    progress >= 1 ? Theme.SUCCESS : Theme.accent());
        } else {
            Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.overview.no_goal"), w - 20), x + 10, y + 26,
                    Theme.TEXT_MUTED);
            String label = I18n.get("multiclicker.gui.overview.set_goal");
            int buttonW = Math.min(w - 20, font.width(label) + 20);
            boolean overButton = over(mouseX, mouseY, mouseInView, x + 10, y + 42, buttonW, 16);
            int base = Theme.alpha(Theme.accent(), 0.85F);
            Draw.rect(g, x + 10, y + 42, buttonW, 16, 3, overButton ? Theme.mix(base, 0xFFFFFFFF, 0.18F) : base);
            Draw.textCentered(g, font, Draw.ellipsize(font, label, buttonW - 8), x + 10 + buttonW / 2, y + 46, 0xFFFFFFFF);
            hits.add(new Hit("set_goal", x + 10, y + 42, buttonW, 16, () -> {
                goals.enabledSetting().set(true);
                showModule(goals);
            }, null, null));
        }
    }

    /** Every module that can be switched, as chips: click switches it, right-click opens its card. */
    private int drawModulesCard(Canvas g, int x, int y, int w, int mouseX, int mouseY, boolean mouseInView) {
        List<Module> modules = new ArrayList<>();
        for (Module module : mod.modules()) {
            if (module.enabledSetting() != null) {
                modules.add(module);
            }
        }
        // Lays the chips out first, to know the height of the card.
        int[] chipX = new int[modules.size()];
        int[] chipY = new int[modules.size()];
        int[] chipW = new int[modules.size()];
        int cx = x + 10;
        int cy = y + 24;
        for (int i = 0; i < modules.size(); i++) {
            int cw = Math.min(w - 20, font.width(modules.get(i).name().getString()) + 20);
            if (cx + cw > x + w - 10 && cx > x + 10) {
                cx = x + 10;
                cy += 20;
            }
            chipX[i] = cx;
            chipY[i] = cy;
            chipW[i] = cw;
            cx += cw + 4;
        }
        int h = cy + 16 + 8 - y;
        Draw.box(g, x, y, w, h, 4, Theme.CARD, Theme.CARD_BORDER);
        int on = 0;
        for (Module module : modules) {
            on += module.isEnabled() ? 1 : 0;
        }
        overviewTitle(g, x, y, w, I18n.get("multiclicker.gui.overview.modules"), Theme.TEXT, null, null, mouseX, mouseY,
                mouseInView);
        Draw.textRight(g, font, I18n.get("multiclicker.gui.overview.modules_on", on, modules.size()), x + w - 10, y + 8,
                Theme.TEXT_MUTED);
        int accent = Theme.accent();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean enabled = module.isEnabled();
            boolean over = over(mouseX, mouseY, mouseInView, chipX[i], chipY[i], chipW[i], 16);
            int fill = enabled ? Theme.alpha(accent, 0.2F) : Theme.CONTROL;
            Draw.rect(g, chipX[i], chipY[i], chipW[i], 16, 3, over ? Theme.mix(fill, 0xFFFFFFFF, 0.1F) : fill);
            Draw.rect(g, chipX[i] + 6, chipY[i] + 6, 4, 4, 2, enabled ? accent : Theme.TEXT_MUTED);
            Draw.text(g, font, Draw.ellipsize(font, module.name().getString(), chipW[i] - 18), chipX[i] + 14, chipY[i] + 4,
                    enabled ? Theme.TEXT : Theme.TEXT_DIM);
            Component tooltip = module.description().copy().append("\n")
                    .append(Component.translatable("multiclicker.gui.overview.module_hint").withStyle(ChatFormatting.GRAY));
            hits.add(new Hit(module, chipX[i], chipY[i], chipW[i], 16, () -> module.enabledSetting().toggle(),
                    () -> showModule(module), tooltip));
        }
        return y + h;
    }

    /** Keys bound to more than one thing, if any. */
    private int drawConflictsCard(Canvas g, int x, int y, int w, int mouseX, int mouseY, boolean mouseInView) {
        Map<String, List<KeyConflicts.Use>> conflicts = KeyConflicts.all();
        if (conflicts.isEmpty()) {
            return y - CARD_GAP;
        }
        int h = 26 + conflicts.size() * 17 + 4;
        Draw.box(g, x, y, w, h, 4, Theme.CARD, Theme.alpha(Theme.WARNING, 0.45F));
        Draw.rect(g, x + 1, y + 8, 2, 16, 1, Theme.WARNING);
        overviewTitle(g, x, y, w, I18n.get("multiclicker.gui.overview.conflicts"), Theme.WARNING, null, null, mouseX, mouseY,
                mouseInView);
        int lineY = y + 24;
        for (Map.Entry<String, List<KeyConflicts.Use>> entry : conflicts.entrySet()) {
            String keyName = Keys.parse(entry.getKey()).getDisplayName().getString();
            int chipW = Draw.chipWidth(font, keyName);
            Draw.chip(g, font, keyName, x + 10, lineY, CHIP_HEIGHT, 0, false, true, true);
            List<Component> names = new ArrayList<>();
            for (KeyConflicts.Use use : entry.getValue()) {
                names.add(use.name());
            }
            Component joined = KeyConflicts.join(names);
            Draw.text(g, font, Draw.ellipsize(font, joined.getString(), w - chipW - 28), x + 16 + chipW, lineY + 3,
                    Theme.TEXT_DIM);
            Runnable open = openerFor(entry.getValue());
            Component tooltip = joined.copy().append("\n").append(Component.translatable("multiclicker.gui.overview.conflict_hint")
                    .withStyle(ChatFormatting.GRAY));
            hits.add(new Hit(entry.getKey(), x + 6, lineY - 1, w - 12, 16, open, null, tooltip));
            lineY += 17;
        }
        return y + h;
    }

    /** Opens where the first of the mod's own uses of a key can be changed. */
    @Nullable
    private Runnable openerFor(List<KeyConflicts.Use> uses) {
        for (KeyConflicts.Use use : uses) {
            if (use.owner() instanceof KeySetting key) {
                return () -> showModule(key.module());
            }
            if (use.owner() instanceof String) {
                return this::openProfiles;
            }
        }
        return null;
    }

    private void openProfiles() {
        Screens.open(minecraft, new ProfilesScreen(this));
    }

    private void openStats() {
        Screens.open(minecraft, new StatsScreen(this));
    }

    // --- Cards ----------------------------------------------------------------------------------

    /** Wraps the module description into at most two lines (the rest is in the tooltip). */
    private void layoutDescription(Card card, int cardW) {
        int textW = cardW - 20 - (card.toggle != null ? 30 : 0) - headerButtonsWidth(card.module);
        if (card.descriptionWidth == textW) {
            return;
        }
        card.descriptionWidth = textW;
        List<String> lines = new ArrayList<>();
        font.getSplitter().splitLines(card.module.description().getString(), textW, Style.EMPTY)
                .forEach(line -> lines.add(line.getString().strip()));
        if (lines.size() > 2) {
            String rest = String.join(" ", lines.subList(1, lines.size()));
            lines.subList(1, lines.size()).clear();
            lines.add(Draw.ellipsize(font, rest, textW));
        }
        card.description = lines.isEmpty() ? List.of("") : lines;
    }

    /** The open/close arrow left of the module name; null when the card has nothing to hide. */
    private int[] chevronBounds(Card card, int x) {
        return new int[] {x + 4, card.y + 3, 14, 15};
    }

    /** A small triangle: pointing down when the card is open, right when closed. */
    private static void drawChevron(Canvas g, int x, int y, boolean down, int color) {
        for (int i = 0; i < 3; i++) {
            if (down) {
                g.fill(x + i, y + 1 + i, x + 5 - i, y + 2 + i, color);
            } else {
                g.fill(x + 1 + i, y + i, x + 2 + i, y + 5 - i, color);
            }
        }
    }

    private Object drawCard(Canvas g, Card card, int x, int w, int mouseX, int mouseY, boolean mouseInView, float delta,
                            Object hovered) {
        Module module = card.module;
        boolean enabled = module.isEnabled();
        int accent = Theme.accent();
        float flashing = flash == module ? 1 - Math.min(1, (System.currentTimeMillis() - flashSince) / (float) FLASH_MS) : 0;
        Draw.box(g, x, card.y, w, card.height, 4, Theme.CARD,
                flashing > 0 ? Theme.mix(Theme.CARD_BORDER, accent, flashing) : Theme.CARD_BORDER);
        if (card.toggle != null) {
            Draw.rect(g, x + 1, card.y + 8, 2, 16, 1, enabled ? accent : Theme.CONTROL_HOVER);
        }

        boolean collapsible = query.isEmpty() && hasBody(card);
        int titleX = x + 10;
        if (collapsible) {
            int[] b = chevronBounds(card, x);
            boolean overChevron = mouseInView && isOver(mouseX, mouseY, b[0], b[1], b[2], b[3]);
            drawChevron(g, x + 8, card.y + 9, isExpanded(card), overChevron ? accent : Theme.TEXT_MUTED);
            if (overChevron) {
                hovered = new Chevron(module);
            }
            titleX = x + 18;
        }
        String prefix = query.isEmpty() ? "" : module.category().icon() + " ";

        int buttonsRight = x + w - 10 - (card.toggle != null ? 30 : 0);
        int textRight = buttonsRight - headerButtonsWidth(module);
        hovered = drawHeaderButtons(g, card, buttonsRight, mouseX, mouseY, mouseInView, delta, hovered);
        g.text(font, Component.literal(prefix + Draw.ellipsize(font, module.name().getString(), textRight - titleX - 2 - font.width(prefix)))
                .withStyle(ChatFormatting.BOLD), titleX, card.y + 7, enabled ? Theme.TEXT : Theme.TEXT_DIM);
        for (int i = 0; i < card.description.size(); i++) {
            Draw.text(g, font, card.description.get(i), x + 10, card.y + 19 + i * DESCRIPTION_LINE, Theme.TEXT_MUTED);
        }
        if (card.toggle != null) {
            card.toggle.render(g, font, x + w - 32, card.y + 4, 24, mouseX, mouseY, delta);
        }
        if (hovered == null && mouseInView && mouseX >= x && mouseX < textRight && mouseY >= card.y
                && mouseY < card.y + card.headerHeight) {
            hovered = module;
        }

        // The sliders of a card share one value column, so their tracks start at the same place.
        int sliderColumn = 0;
        for (Row row : card.rows) {
            if (row.visible && row.setting != null && control(row.setting) instanceof SliderControl slider) {
                sliderColumn = Math.max(sliderColumn, slider.widestValue(font));
            }
        }

        boolean first = true;
        for (Row row : card.rows) {
            if (!row.visible) {
                continue;
            }
            if (first) {
                g.fill(x + 8, row.y - 1, x + w - 8, row.y, Theme.DIVIDER);
                first = false;
            }
            if (row.subheader != null) {
                String label = row.subheader.getString().toUpperCase(Locale.ROOT);
                Draw.text(g, font, label, x + 10, row.y + 6, Theme.alpha(accent, 0.9F));
                int lineX = x + 16 + font.width(label);
                g.fill(lineX, row.y + 10, x + w - 10, row.y + 11, Theme.DIVIDER);
                continue;
            }
            if (row.button != null) {
                int buttonW = Math.min(w - 20, font.width(row.button.label.getString()) + 24);
                row.button.bounds(x + 10, row.y + 4, buttonW, 15).render(g, font, mouseX, mouseY, delta);
                continue;
            }
            Setting<?> setting = row.setting;
            boolean overRow = mouseInView && mouseX >= x + 1 && mouseX < x + w - 1 && mouseY >= row.y && mouseY < row.y + ROW;
            boolean isFocused = setting == focused;
            SettingControl control = control(setting);
            if (control instanceof SliderControl slider) {
                slider.column(font, sliderColumn);
            }
            control.fit(font, (w - 20) * 11 / 20);
            int controlW = control.width(font);
            int controlX = x + w - 10 - controlW;
            String name = setting.name().getString();
            String label = Draw.ellipsize(font, name, controlX - x - 20);
            if (overRow || isFocused) {
                g.fill(x + 1, row.y, x + w - 1, row.y + ROW, isFocused ? Theme.alpha(accent, 0.12F) : Theme.ROW_HOVER);
            }
            if (isFocused) {
                Draw.rect(g, x + 1, row.y + 3, 2, ROW - 6, 1, accent);
            }
            if (overRow) {
                hovered = setting;
                hoveredNameCut = !label.equals(name);
            }
            if (!setting.isDefault() && !(setting instanceof KeySetting)) {
                // Changed from the default: right-click the row resets it.
                Draw.rect(g, x + 5, row.y + 8, 2, 2, 1, Theme.alpha(accent, enabled ? 0.9F : 0.5F));
            }
            Draw.text(g, font, label, x + 10, row.y + 5, enabled ? Theme.TEXT : Theme.TEXT_DIM);
            control.render(g, font, controlX, row.y, ROW, mouseX, mouseY, delta);
        }
        return hovered;
    }

    /** What a header button stands for, for its tooltip and hover animation. */
    private record HeaderButton(Module module, boolean reset) {
    }

    /** The open/close arrow of a card, for its tooltip. */
    private record Chevron(Module module) {
    }

    private String chipText(KeySetting key) {
        if (capture.isWaitingFor(key)) {
            return I18n.get("multiclicker.key.press");
        }
        return key.isBound() ? key.displayValue() : null;
    }

    /** Room taken by the hotkey chip and the reset button in a module header. */
    private int headerButtonsWidth(Module module) {
        int width = 0;
        if (module.keySetting() != null) {
            width += Draw.chipWidth(font, chipText(module.keySetting())) + 6;
        }
        if (isModified(module)) {
            width += 14;
        }
        return width;
    }

    /** Whether any setting of the module (other than its switch and hotkey) differs from the default. */
    private static boolean isModified(Module module) {
        for (Setting<?> setting : module.settings()) {
            if (setting != module.enabledSetting() && !setting.isGlobal() && !setting.isInternal() && !setting.isDefault()) {
                return true;
            }
        }
        return false;
    }

    private void resetModule(Module module) {
        for (Setting<?> setting : module.settings()) {
            if (setting != module.enabledSetting() && !setting.isGlobal() && !setting.isInternal()) {
                setting.reset();
            }
        }
    }

    private Object drawHeaderButtons(Canvas g, Card card, int right, int mouseX, int mouseY, boolean mouseInView,
                                     float delta, Object hovered) {
        Module module = card.module;
        KeySetting key = module.keySetting();
        int buttonY = card.y + 9;
        card.chipX = -1;
        card.resetX = -1;
        if (key != null) {
            String text = chipText(key);
            card.chipW = Draw.chipWidth(font, text);
            card.chipX = right - card.chipW;
            boolean over = mouseInView && isOver(mouseX, mouseY, card.chipX, buttonY, card.chipW, CHIP_HEIGHT);
            float h = headerHover.computeIfAbsent(new HeaderButton(module, false), k -> new Anim(0)).update(over ? 1 : 0, delta, 20);
            boolean showChip = key.isBound() || capture.isWaitingFor(key) || h > 0.01F;
            if (showChip) {
                Draw.chip(g, font, text, card.chipX, buttonY, CHIP_HEIGHT, h, capture.isWaitingFor(key), key.isBound(),
                        !KeyConflicts.others(key).isEmpty());
            } else {
                // An unbound hotkey stays a faint icon until the mouse comes near.
                Draw.keyboardIcon(g, card.chipX + (card.chipW - 9) / 2, buttonY + 4, Theme.alpha(Theme.TEXT_MUTED, 0.7F));
            }
            if (over) {
                hovered = new HeaderButton(module, false);
            }
            right = card.chipX - 6;
        }
        if (isModified(module)) {
            card.resetX = right - 12;
            boolean over = mouseInView && isOver(mouseX, mouseY, card.resetX - 1, buttonY, 14, CHIP_HEIGHT);
            float h = headerHover.computeIfAbsent(new HeaderButton(module, true), k -> new Anim(0)).update(over ? 1 : 0, delta, 20);
            Draw.textCentered(g, font, "↺", card.resetX + 6, buttonY + 3, Theme.mix(Theme.TEXT_MUTED, Theme.accent(), h));
            if (over) {
                hovered = new HeaderButton(module, true);
            }
        }
        return hovered;
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Tips in the footer take turns, fading from one to the next. */
    private void renderFooter(Canvas g) {
        int y = panelY + panelH - FOOTER + 4;
        g.fill(viewX + 1, panelY + panelH - FOOTER, panelX + panelW - 1, panelY + panelH - FOOTER + 1, Theme.DIVIDER);
        long elapsed = System.currentTimeMillis() - openedAt;
        int index = (int) (elapsed / HINT_MS % HINTS.length);
        long phase = elapsed % HINT_MS;
        float visible = Math.min(1, Math.min(phase / 300.0F, (HINT_MS - phase) / 300.0F));
        if (elapsed < HINT_MS) {
            visible = Math.min(1, (HINT_MS - phase) / 300.0F); // no fade-in when the menu opens
        }
        String hint = I18n.get(HINTS[index], mod.toggleKey.getTranslatedKeyMessage().getString());
        Draw.textCentered(g, font, Draw.ellipsize(font, hint, viewW - 16), viewX + viewW / 2, y,
                Theme.mix(Theme.PANEL, Theme.TEXT_MUTED, visible));
    }

    private void renderTooltip(Canvas g, int mouseX, int mouseY, @Nullable Object hovered) {
        if (!java.util.Objects.equals(hovered, hoverKey)) {
            hoverKey = hovered;
            hoverSince = System.currentTimeMillis();
        }
        if (hovered == null || dragging != null || System.currentTimeMillis() - hoverSince < TOOLTIP_DELAY_MS) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (hovered instanceof Setting<?> setting) {
            if (hoveredNameCut) {
                lines.addAll(font.split(setting.name(), 220));
            }
            Component description = setting.description();
            if (description != null) {
                lines.addAll(font.split(hoveredNameCut ? description.copy().withStyle(ChatFormatting.GRAY) : description, 220));
            }
            if (setting instanceof KeySetting key) {
                addConflicts(lines, KeyConflicts.others(key));
            } else if (!setting.isDefault()) {
                lines.addAll(font.split(Component.translatable("multiclicker.gui.default", setting.displayDefault())
                        .withStyle(ChatFormatting.DARK_GRAY), 220));
            }
        } else if (hovered instanceof HeaderButton button) {
            lines.addAll(font.split(Component.translatable(button.reset() ? "multiclicker.gui.reset_module"
                    : "multiclicker.gui.module_key", button.module().name()), 220));
            if (!button.reset() && button.module().keySetting() != null) {
                addConflicts(lines, KeyConflicts.others(button.module().keySetting()));
            }
        } else if (hovered instanceof Chevron chevron) {
            lines.addAll(font.split(Component.translatable("multiclicker.gui.collapse"), 220));
        } else if (hovered instanceof Module module) {
            lines.addAll(font.split(module.description(), 220));
        } else if (hovered instanceof Hit hit && hit.tooltip() != null) {
            lines.addAll(font.split(hit.tooltip(), 220));
        } else if (hovered instanceof Category entry) {
            lines.addAll(font.split(entry.title(), 220));
        } else if ("overview".equals(hovered)) {
            lines.addAll(font.split(sidebarTooltip("multiclicker.gui.overview"), 220));
        } else if ("stats".equals(hovered)) {
            lines.addAll(font.split(sidebarTooltip("multiclicker.gui.stats"), 220));
        } else if ("profiles".equals(hovered)) {
            lines.addAll(font.split(sidebarTooltip("multiclicker.gui.profiles"), 220));
        }
        if (!lines.isEmpty()) {
            Gfx.tooltip(g, font, lines, mouseX, mouseY);
        }
    }

    private void addConflicts(List<FormattedCharSequence> lines, List<Component> others) {
        if (!others.isEmpty()) {
            lines.addAll(font.split(Component.translatable("multiclicker.gui.conflict", KeyConflicts.join(others))
                    .withStyle(ChatFormatting.GOLD), 220));
        }
    }

    /** The description of a sidebar link; with its name too when the sidebar shows only icons. */
    private Component sidebarTooltip(String key) {
        Component description = Component.translatable(key + ".desc");
        return compactSidebar ? Component.translatable(key).append(" — ").append(description) : description;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - viewH);
    }

    // --- Input ----------------------------------------------------------------------------------

    /** Applies (or drops) a number being typed into a slider. */
    private void finishEditing(boolean apply) {
        if (editing != null) {
            if (apply) {
                editing.commit();
            } else {
                editing.cancelEdit();
            }
            editing = null;
        }
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        if (capture.isActive() && button >= 2) {
            capture.mouseClicked(button);
            playClick();
            return true;
        }
        capture.cancel();
        if (editing != null && !(editing.isOverValue(mouseX, mouseY) && button == 0)) {
            finishEditing(true);
        }
        boolean overSearch = mouseX >= searchX && mouseX < searchX + searchW && mouseY >= panelY + 7 && mouseY < panelY + 23;
        if (overSearch) {
            setFocused(search);
            Widgets.focus(search, true);
            if (button == 1) {
                search.setValue("");
            }
            super.clicked(mouseX, mouseY, button);
            return true;
        }
        Widgets.focus(search, false);
        setFocused(null);

        if (isOverMaster(mouseX, mouseY) && button == 0 && minecraft.player != null) {
            mod.setActive(!mod.isActive(), null);
            return true;
        }
        if (isOverSidebarItem(mouseX, mouseY, sidebarTop())) {
            showOverview();
            playClick();
            return true;
        }
        int y = categoriesTop();
        for (Category entry : Category.values()) {
            if (isOverSidebarItem(mouseX, mouseY, y)) {
                showCategory(entry);
                playClick();
                return true;
            }
            y += sidebarItem;
        }
        if (isOverSidebarItem(mouseX, mouseY, statsItemY())) {
            playClick();
            openStats();
            return true;
        }
        if (isOverSidebarItem(mouseX, mouseY, profilesItemY())) {
            playClick();
            openProfiles();
            return true;
        }

        boolean inView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
        if (inView) {
            if (maxScroll() > 0 && mouseX >= viewX + viewW - 7) {
                draggingScrollbar = true;
                scrollFromMouse(mouseY);
                return true;
            }
            if (isOverview()) {
                for (Hit hit : List.copyOf(hits)) {
                    Runnable action = button == 0 ? hit.left() : button == 1 ? hit.right() : null;
                    if (action != null && hit.contains(mouseX, mouseY)) {
                        playClick();
                        action.run();
                        return true;
                    }
                }
                return super.clicked(mouseX, mouseY, button);
            }
            int cardX = viewX + CARD_MARGIN;
            int cardW = viewW - CARD_MARGIN * 2 - 4;
            for (Card card : cards) {
                if (clickedHeaderButton(card, mouseX, mouseY, button)) {
                    playClick();
                    return true;
                }
                if (query.isEmpty() && hasBody(card) && button == 0) {
                    int[] b = chevronBounds(card, cardX);
                    if (isOver(mouseX, mouseY, b[0], b[1], b[2], b[3])) {
                        EXPANDED.put(card.module.id(), !isExpanded(card));
                        playClick();
                        return true;
                    }
                }
                BoolSetting enabled = card.module.enabledSetting();
                boolean overHeader = mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= card.y && mouseY < card.y + card.headerHeight;
                if (overHeader && (button == 0 || button == 1)) {
                    if (enabled != null) {
                        // The whole header acts as the module switch.
                        if (button == 0) {
                            enabled.toggle();
                        } else {
                            enabled.reset();
                        }
                    } else if (button == 0 && query.isEmpty() && hasBody(card)) {
                        EXPANDED.put(card.module.id(), !isExpanded(card));
                    }
                    playClick();
                    return true;
                }
                for (Row row : card.rows) {
                    if (!row.visible || mouseY < row.y || mouseY >= row.y + row.height()) {
                        continue;
                    }
                    if (row.button != null && row.button.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                    if (row.setting == null) {
                        continue;
                    }
                    SettingControl control = control(row.setting);
                    if (control.mouseClicked(mouseX, mouseY, button)) {
                        dragging = control;
                        if (control instanceof SliderControl slider && slider.isEditing()) {
                            editing = slider;
                        }
                        playClick();
                        return true;
                    }
                    if (row.setting instanceof BoolSetting bool && mouseX >= cardX && mouseX < cardX + cardW) {
                        // A click anywhere on a switch row flips the switch.
                        if (button == 0) {
                            bool.toggle();
                        } else {
                            bool.reset();
                        }
                        playClick();
                        return true;
                    }
                }
            }
        }
        return super.clicked(mouseX, mouseY, button);
    }

    private boolean clickedHeaderButton(Card card, double mouseX, double mouseY, int button) {
        int buttonY = card.y + 9;
        KeySetting key = card.module.keySetting();
        if (key != null && card.chipX >= 0 && isOver(mouseX, mouseY, card.chipX, buttonY, card.chipW, CHIP_HEIGHT)) {
            if (button == 0) {
                capture.start(key, key::bind);
            } else if (button == 1) {
                key.clear();
            }
            return true;
        }
        if (card.resetX >= 0 && button == 0 && isOver(mouseX, mouseY, card.resetX - 1, buttonY, 14, CHIP_HEIGHT)) {
            resetModule(card.module);
            return true;
        }
        return false;
    }

    @Override
    protected boolean dragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        if (dragging != null && dragging.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.dragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean released(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        if (dragging != null) {
            dragging.mouseReleased();
            dragging = null;
        }
        return super.released(mouseX, mouseY, button);
    }

    @Override
    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hoverKey instanceof Setting<?> setting && control(setting).mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 28, 0, maxScroll());
            return true;
        }
        return super.scrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        if (capture.keyPressed(keyCode)) {
            // Also takes Escape, which would otherwise close the menu.
            return true;
        }
        if (editing != null) {
            switch (keyCode) {
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_TAB -> finishEditing(true);
                case InputConstants.KEY_ESCAPE -> finishEditing(false);
                case InputConstants.KEY_BACKSPACE -> editing.backspace();
                default -> {
                }
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB && !search.isFocused()) {
            // Tab and Shift+Tab step through the overview and the categories.
            int pages = Category.values().length + 1;
            int current = category == null || !query.isEmpty() && category == null ? 0 : category.ordinal() + 1;
            int next = Math.floorMod(current + (Keys.shiftDown() ? -1 : 1), pages);
            showCategory(next == 0 ? null : Category.values()[next - 1]);
            playClick();
            return true;
        }
        if (keyCode == InputConstants.KEY_F && Keys.controlDown()) {
            setFocused(search);
            Widgets.focus(search, true);
            return true;
        }
        if (search.isFocused()) {
            if (keyCode == InputConstants.KEY_DOWN && !navigation.isEmpty()) {
                // From the search field down into the results.
                Widgets.focus(search, false);
                setFocused(null);
                moveFocus(1);
                return true;
            }
            return super.pressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN) {
            moveFocus(keyCode == InputConstants.KEY_DOWN ? 1 : -1);
            return true;
        }
        Setting<?> target = focused != null ? focused : hoverKey instanceof Setting<?> setting ? setting : null;
        if (target != null && (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT)) {
            return control(target).adjust(keyCode == InputConstants.KEY_RIGHT ? 1 : -1);
        }
        if (focused != null && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER
                || keyCode == InputConstants.KEY_SPACE)) {
            activate(focused);
            return true;
        }
        if (focused != null && keyCode == InputConstants.KEY_BACKSPACE && !(focused instanceof KeySetting)) {
            focused.reset();
            return true;
        }
        return super.pressed(keyCode, scanCode, modifiers);
    }

    /** Moves the keyboard focus to the next or previous setting row and scrolls it into view. */
    private void moveFocus(int direction) {
        if (navigation.isEmpty()) {
            return;
        }
        int index = -1;
        for (int i = 0; i < navigation.size(); i++) {
            if (navigation.get(i).setting == focused) {
                index = i;
            }
        }
        if (index < 0) {
            // Starts from the first row on screen.
            index = direction > 0 ? -1 : navigation.size();
            for (int i = 0; i < navigation.size(); i++) {
                if (navigation.get(i).y >= viewY) {
                    index = direction > 0 ? i - 1 : i + 1;
                    break;
                }
            }
        }
        Row row = navigation.get(Mth.clamp(index + direction, 0, navigation.size() - 1));
        focused = row.setting;
        if (row.y < viewY + 4) {
            scrollTarget -= viewY + 4 - row.y;
        } else if (row.y + ROW > viewY + viewH - 4) {
            scrollTarget += row.y + ROW - (viewY + viewH - 4);
        }
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll());
        playClick();
    }

    /** Enter or Space on the focused row: flips a switch, cycles a choice, starts typing a number. */
    private void activate(Setting<?> setting) {
        if (setting instanceof BoolSetting bool) {
            bool.toggle();
        } else {
            SettingControl control = control(setting);
            control.activate();
            if (control instanceof SliderControl slider && slider.isEditing()) {
                editing = slider;
            }
        }
        playClick();
    }

    @Override
    protected boolean typed(char codePoint, int modifiers) {
        if (capture.swallowTyped()) {
            return true;
        }
        if (editing != null) {
            editing.type(codePoint);
            return true;
        }
        if (codePoint == ' ' && focused != null) {
            return true;
        }
        // Typing anywhere starts a search.
        if (!search.isFocused() && Character.isLetterOrDigit(codePoint)) {
            setFocused(search);
            Widgets.focus(search, true);
        }
        return super.typed(codePoint, modifiers);
    }

    private void scrollFromMouse(double mouseY) {
        float t = (float) ((mouseY - viewY) / viewH);
        scrollTarget = scroll = Mth.clamp(t * contentHeight - viewH / 2.0F, 0, maxScroll());
    }

    private void playClick() {
        minecraft.getSoundManager().play(Sounds.click(1.0F, 0.35F));
    }
}
