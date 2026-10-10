package io.github.shamanalle.multiclicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
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
import io.github.shamanalle.multiclicker.gui.widget.SettingControl;
import io.github.shamanalle.multiclicker.gui.widget.SliderControl;
import io.github.shamanalle.multiclicker.gui.widget.ToggleControl;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.KeySetting;
import io.github.shamanalle.multiclicker.setting.Setting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main settings menu: categories on the left, one card per module on the right, a search
 * field and the master on/off switch in the header.
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
    private static final int CARD_GAP = 6;
    private static final int CARD_MARGIN = 8;
    private static final long TOOLTIP_DELAY_MS = 350;
    private static final String[] CLICKER_GROUPS = {"attack", "use", "jump", "general"};
    private static final String[] HINTS = {"multiclicker.gui.hint", "multiclicker.gui.hint.value",
            "multiclicker.gui.hint.keys", "multiclicker.gui.hint.search"};
    private static final long HINT_MS = 6000;
    private static final int CHIP_HEIGHT = 14;

    private static Category lastCategory = Category.CLICKER;

    @Nullable
    private final Screen parent;
    private final MultiClicker mod = MultiClicker.get();
    private final List<Card> cards = new ArrayList<>();
    private final Map<Setting<?>, SettingControl> controls = new HashMap<>();
    private final Map<Category, Anim> categoryHover = new EnumMap<>(Category.class);
    private final Anim profilesHover = new Anim(0);
    private final Anim masterAnim;
    private final Map<Object, Anim> headerHover = new HashMap<>();
    /** Slides the cards in when another category is shown. */
    private final Anim contentShift = new Anim(0);
    private final KeyCapture capture = new KeyCapture();
    private final long openedAt = System.currentTimeMillis();
    /** The slider whose number is being typed, if any. */
    @Nullable
    private SliderControl editing;

    private Category category = lastCategory;
    private String query = "";
    private EditBox search;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarW;
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

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("multiclicker.gui.title"));
        this.parent = parent;
        this.masterAnim = new Anim(mod.isActive() ? 1 : 0);
    }

    /** Shows a category and clears the search. */
    public void showCategory(Category target) {
        category = lastCategory = target;
        query = "";
        if (search != null) {
            search.setValue("");
        }
        rebuild();
        contentShift.set(1);
    }

    /** Fills the search field, as if the player typed the text. */
    public void search(String text) {
        if (search != null) {
            search.setValue(text);
        }
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
        int y;
        boolean visible;

        Row(@Nullable Setting<?> setting, @Nullable Component subheader) {
            this.setting = setting;
            this.subheader = subheader;
        }

        int height() {
            return subheader != null ? SUBHEADER : ROW;
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
                    rows.add(new Row(setting, null));
                }
                continue;
            }
            int group = groupStarts.indexOf(setting);
            if (group >= 0) {
                rows.add(new Row(null, Component.translatable("multiclicker.group." + CLICKER_GROUPS[group])));
            }
            rows.add(new Row(setting, null));
        }
    }

    private static boolean matches(Component text, String query) {
        return text.getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** The English ids ("auto_eat", "attack_interval") also match, whatever the game language. */
    private static boolean matchesId(String id, String query) {
        return id.replace('_', ' ').contains(query) || id.contains(query);
    }

    // --- Setup ----------------------------------------------------------------------------------

    @Override
    protected void init() {
        panelW = Math.min(width - 24, 560);
        panelH = Math.min(height - 24, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        // The sidebar fits its longest label; on small screens it shrinks to icons with tooltips.
        int widestLabel = font.width(I18n.get("multiclicker.gui.profiles"));
        for (Category entry : Category.values()) {
            widestLabel = Math.max(widestLabel, font.width(entry.title().getString()));
        }
        int fullSidebar = Mth.clamp(widestLabel + 46, 96, 150);
        compactSidebar = panelW - fullSidebar < 240;
        sidebarW = compactSidebar ? COMPACT_SIDEBAR : fullSidebar;
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
        searchW = Mth.clamp(masterX - 8 - titleEnd, 60, 150);
        searchX = masterX - 8 - searchW;

        search = new EditBox(font, searchX + 6, panelY + 11, searchW - 12, 10, Component.translatable("multiclicker.gui.search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setTextColor(Theme.TEXT);
        search.setHint(Component.translatable("multiclicker.gui.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(value -> {
            if (!value.equals(query)) {
                query = value;
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

    private int profilesItemY() {
        return panelY + panelH - SIDEBAR_ITEM - 6;
    }

    private Object renderSidebar(Canvas g, int mouseX, int mouseY, float delta, Object hovered) {
        int y = sidebarTop();
        int accent = Theme.accent();
        for (Category entry : Category.values()) {
            boolean selected = query.isEmpty() && entry == category;
            boolean over = isOverSidebarItem(mouseX, mouseY, y);
            float h = categoryHover.computeIfAbsent(entry, c -> new Anim(0)).update(over || selected ? 1 : 0, delta, 18);
            if (h > 0) {
                int fill = selected ? Theme.alpha(accent, 0.16F) : Theme.alpha(0xFFFFFFFF, 0.05F * h);
                Draw.rect(g, panelX + 4, y, sidebarW - 8, SIDEBAR_ITEM - 2, 3, fill);
            }
            if (selected) {
                Draw.rect(g, panelX + 4, y + 4, 2, SIDEBAR_ITEM - 10, 1, accent);
            }
            int count = enabledCount(entry);
            if (compactSidebar) {
                Draw.textCentered(g, font, entry.icon(), panelX + sidebarW / 2, y + 5,
                        selected ? accent : count > 0 ? Theme.TEXT_DIM : Theme.TEXT_MUTED);
                if (over) {
                    hovered = entry;
                }
            } else {
                int textColor = selected ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h);
                Draw.text(g, font, entry.icon(), panelX + 12, y + 5, selected ? accent : Theme.TEXT_MUTED);
                String countText = count > 0 ? Integer.toString(count) : "";
                int maxLabel = sidebarW - 38 - font.width(countText);
                Draw.text(g, font, Draw.ellipsize(font, entry.title().getString(), maxLabel), panelX + 24, y + 5, textColor);
                if (count > 0) {
                    Draw.textRight(g, font, countText, panelX + sidebarW - 10, y + 5, Theme.alpha(accent, selected ? 1 : 0.7F));
                }
            }
            y += SIDEBAR_ITEM;
        }

        int profilesY = profilesItemY();
        g.fill(panelX + 8, profilesY - 5, panelX + sidebarW - 8, profilesY - 4, Theme.DIVIDER);
        boolean over = isOverSidebarItem(mouseX, mouseY, profilesY);
        float h = profilesHover.update(over ? 1 : 0, delta, 18);
        if (h > 0) {
            Draw.rect(g, panelX + 4, profilesY, sidebarW - 8, SIDEBAR_ITEM - 2, 3, Theme.alpha(0xFFFFFFFF, 0.05F * h));
        }
        int iconColor = Theme.mix(Theme.TEXT_MUTED, accent, h);
        if (compactSidebar) {
            Draw.textCentered(g, font, "☰", panelX + sidebarW / 2, profilesY + 5, iconColor);
        } else {
            Draw.text(g, font, "☰", panelX + 12, profilesY + 5, iconColor);
            Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.profiles"), sidebarW - 34),
                    panelX + 24, profilesY + 5, Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h));
        }
        if (over) {
            hovered = "profiles";
        }
        return hovered;
    }

    private boolean isOverSidebarItem(double mouseX, double mouseY, int itemY) {
        return mouseX >= panelX + 4 && mouseX < panelX + sidebarW - 4 && mouseY >= itemY && mouseY < itemY + SIDEBAR_ITEM - 2;
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
        int y = viewY + CARD_MARGIN - Math.round(scroll) + slide;
        for (Card card : cards) {
            card.y = y;
            layoutDescription(card, cardW);
            card.headerHeight = CARD_HEADER + (card.description.size() - 1) * DESCRIPTION_LINE;
            int height = card.headerHeight;
            boolean anyRow = false;
            for (Row row : card.rows) {
                row.visible = row.setting == null || row.setting.isVisible();
                if (row.visible) {
                    row.y = y + height;
                    height += row.height();
                    anyRow = true;
                }
            }
            card.height = height + (anyRow ? 5 : 0);
            if (card.y + card.height > viewY && card.y < viewY + viewH) {
                hovered = drawCard(g, card, cardX, cardW, mouseX, mouseY, mouseInView, delta, hovered);
            }
            y += card.height + CARD_GAP;
        }
        contentHeight = y - slide + Math.round(scroll) - viewY + CARD_MARGIN - CARD_GAP;
        if (cards.isEmpty()) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.nothing_found"), viewX + viewW / 2, viewY + viewH / 2 - 4,
                    Theme.TEXT_MUTED);
        }
        g.disableScissor();

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

    private Object drawCard(Canvas g, Card card, int x, int w, int mouseX, int mouseY, boolean mouseInView, float delta,
                            Object hovered) {
        Module module = card.module;
        boolean enabled = module.isEnabled();
        int accent = Theme.accent();
        Draw.box(g, x, card.y, w, card.height, 4, Theme.CARD, Theme.CARD_BORDER);
        if (card.toggle != null) {
            Draw.rect(g, x + 1, card.y + 8, 2, 16, 1, enabled ? accent : Theme.CONTROL_HOVER);
        }

        int buttonsRight = x + w - 10 - (card.toggle != null ? 30 : 0);
        int textRight = buttonsRight - headerButtonsWidth(module);
        hovered = drawHeaderButtons(g, card, buttonsRight, mouseX, mouseY, mouseInView, delta, hovered);
        g.text(font, Component.literal(Draw.ellipsize(font, module.name().getString(), textRight - x - 12))
                .withStyle(ChatFormatting.BOLD), x + 10, card.y + 7, enabled ? Theme.TEXT : Theme.TEXT_DIM);
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
            Setting<?> setting = row.setting;
            boolean overRow = mouseInView && mouseX >= x + 1 && mouseX < x + w - 1 && mouseY >= row.y && mouseY < row.y + ROW;
            SettingControl control = control(setting);
            control.fit(font, (w - 20) * 11 / 20);
            int controlW = control.width(font);
            int controlX = x + w - 10 - controlW;
            String name = setting.name().getString();
            String label = Draw.ellipsize(font, name, controlX - x - 20);
            if (overRow) {
                g.fill(x + 1, row.y, x + w - 1, row.y + ROW, Theme.ROW_HOVER);
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
                Draw.chip(g, font, text, card.chipX, buttonY, CHIP_HEIGHT, h, capture.isWaitingFor(key), key.isBound());
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
        if (hovered != hoverKey) {
            hoverKey = hovered;
            hoverSince = System.currentTimeMillis();
        }
        if (hovered == null || dragging != null || System.currentTimeMillis() - hoverSince < TOOLTIP_DELAY_MS) {
            return;
        }
        Component text = null;
        if (hovered instanceof Setting<?> setting) {
            text = setting.description();
            if (hoveredNameCut) {
                List<FormattedCharSequence> lines = new ArrayList<>(font.split(setting.name(), 220));
                if (text != null) {
                    lines.addAll(font.split(text.copy().withStyle(ChatFormatting.GRAY), 220));
                }
                Gfx.tooltip(g, font, lines, mouseX, mouseY);
                return;
            }
        } else if (hovered instanceof HeaderButton button) {
            text = Component.translatable(button.reset() ? "multiclicker.gui.reset_module" : "multiclicker.gui.module_key",
                    button.module().name());
        } else if (hovered instanceof Module module) {
            text = module.description();
        } else if (hovered instanceof Category entry) {
            text = entry.title();
        } else if ("profiles".equals(hovered)) {
            text = compactSidebar
                    ? Component.translatable("multiclicker.gui.profiles").append(" — ").append(Component.translatable("multiclicker.gui.profiles.desc"))
                    : Component.translatable("multiclicker.gui.profiles.desc");
        }
        if (text != null) {
            Gfx.tooltip(g, font, font.split(text, 220), mouseX, mouseY);
        }
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
            search.setFocused(true);
            if (button == 1) {
                search.setValue("");
            }
            super.clicked(mouseX, mouseY, button);
            return true;
        }
        search.setFocused(false);
        setFocused(null);

        if (isOverMaster(mouseX, mouseY) && button == 0 && minecraft.player != null) {
            mod.setActive(!mod.isActive(), null);
            return true;
        }
        int y = sidebarTop();
        for (Category entry : Category.values()) {
            if (isOverSidebarItem(mouseX, mouseY, y)) {
                showCategory(entry);
                playClick();
                return true;
            }
            y += SIDEBAR_ITEM;
        }
        if (isOverSidebarItem(mouseX, mouseY, profilesItemY())) {
            playClick();
            Screens.open(minecraft, new ProfilesScreen(this));
            return true;
        }

        boolean inView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
        if (inView) {
            if (maxScroll() > 0 && mouseX >= viewX + viewW - 7) {
                draggingScrollbar = true;
                scrollFromMouse(mouseY);
                return true;
            }
            int cardX = viewX + CARD_MARGIN;
            int cardW = viewW - CARD_MARGIN * 2 - 4;
            for (Card card : cards) {
                if (clickedHeaderButton(card, mouseX, mouseY, button)) {
                    playClick();
                    return true;
                }
                BoolSetting enabled = card.module.enabledSetting();
                boolean overHeader = mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= card.y && mouseY < card.y + card.headerHeight;
                if (enabled != null && overHeader && (button == 0 || button == 1)) {
                    // The whole header acts as the module switch.
                    if (button == 0) {
                        enabled.toggle();
                    } else {
                        enabled.reset();
                    }
                    playClick();
                    return true;
                }
                for (Row row : card.rows) {
                    if (row.visible && row.setting != null && mouseY >= row.y && mouseY < row.y + ROW) {
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
            // Tab and Shift+Tab step through the categories.
            int next = Math.floorMod(category.ordinal() + (Keys.shiftDown() ? -1 : 1), Category.values().length);
            showCategory(Category.values()[next]);
            playClick();
            return true;
        }
        if (keyCode == InputConstants.KEY_F && Keys.controlDown()) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (!search.isFocused() && hoverKey instanceof Setting<?> setting
                && (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT)) {
            return control(setting).adjust(keyCode == InputConstants.KEY_RIGHT ? 1 : -1);
        }
        return super.pressed(keyCode, scanCode, modifiers);
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
        // Typing anywhere starts a search.
        if (!search.isFocused() && Character.isLetterOrDigit(codePoint)) {
            setFocused(search);
            search.setFocused(true);
        }
        return super.typed(codePoint, modifiers);
    }

    private void scrollFromMouse(double mouseY) {
        float t = (float) ((mouseY - viewY) / viewH);
        scrollTarget = scroll = Mth.clamp(t * contentHeight - viewH / 2.0F, 0, maxScroll());
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.35F));
    }
}
