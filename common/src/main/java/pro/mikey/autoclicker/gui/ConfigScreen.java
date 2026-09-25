package pro.mikey.autoclicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.gui.widget.SettingControl;
import pro.mikey.autoclicker.gui.widget.ToggleControl;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.module.clicker.ClickerModule;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.Setting;

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
public class ConfigScreen extends Screen {
    private static final int HEADER = 30;
    private static final int FOOTER = 16;
    private static final int SIDEBAR_ITEM = 20;
    private static final int CARD_HEADER = 32;
    private static final int ROW = 18;
    private static final int SUBHEADER = 17;
    private static final int CARD_GAP = 6;
    private static final int CARD_MARGIN = 8;
    private static final long TOOLTIP_DELAY_MS = 350;
    private static final String[] CLICKER_GROUPS = {"attack", "use", "jump", "general"};

    private static Category lastCategory = Category.CLICKER;

    @Nullable
    private final Screen parent;
    private final MultiClicker mod = MultiClicker.get();
    private final List<Card> cards = new ArrayList<>();
    private final Map<Setting<?>, SettingControl> controls = new HashMap<>();
    private final Map<Category, Anim> categoryHover = new EnumMap<>(Category.class);
    private final Anim profilesHover = new Anim(0);
    private final Anim masterAnim;

    private Category category = lastCategory;
    private String query = "";
    private EditBox search;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarW;
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

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("multiclicker.gui.title"));
        this.parent = parent;
        this.masterAnim = new Anim(mod.isActive() ? 1 : 0);
    }

    // --- Model ----------------------------------------------------------------------------------

    private static final class Card {
        final Module module;
        final List<Row> rows;
        @Nullable
        final ToggleControl toggle;
        int y;
        int height;

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
        return controls.computeIfAbsent(setting, s -> SettingControl.of(s, this));
    }

    private void rebuild() {
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
                boolean moduleMatches = matches(module.name(), q) || matches(module.description(), q);
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
            if (setting == module.enabledSetting()) {
                continue;
            }
            if (filter != null) {
                if (matches(setting.name(), filter) || setting.description() != null && matches(setting.description(), filter)) {
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

    // --- Setup ----------------------------------------------------------------------------------

    @Override
    protected void init() {
        panelW = Math.min(width - 24, 540);
        panelH = Math.min(height - 24, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sidebarW = panelW < 420 ? 96 : 118;
        viewX = panelX + sidebarW;
        viewY = panelY + HEADER;
        viewW = panelW - sidebarW;
        viewH = panelH - HEADER - FOOTER;

        masterW = Math.max(font.width(I18n.get("multiclicker.gui.active")), font.width(I18n.get("multiclicker.gui.inactive"))) + 24;
        masterX = panelX + panelW - masterW - 8;
        int titleEnd = panelX + 15 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 5
                + font.width("v" + mod.version()) + 12;
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
        mod.saveConfig();
        minecraft.setScreen(parent);
    }

    // --- Rendering ------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0 ? 0 : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = now;

        Draw.box(g, panelX, panelY, panelW, panelH, 4, Theme.PANEL, Theme.CARD_BORDER);
        Draw.rect(g, panelX + 1, panelY + 1, sidebarW - 1, panelH - 2, 3, Theme.SIDEBAR);
        g.fill(panelX + sidebarW, panelY + 1, panelX + sidebarW + 1, panelY + panelH - 1, Theme.DIVIDER);
        g.fill(panelX + sidebarW + 1, panelY + HEADER - 1, panelX + panelW - 1, panelY + HEADER, Theme.DIVIDER);

        Object hovered = null;
        renderHeader(g, mouseX, mouseY, delta);
        hovered = renderSidebar(g, mouseX, mouseY, delta, hovered);
        hovered = renderContent(g, mouseX, mouseY, delta, hovered);
        renderFooter(g);

        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY, hovered);
    }

    private void renderHeader(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int textY = panelY + 11;
        int accent = Theme.accent();
        Draw.rect(g, panelX + 8, panelY + 10, 3, 10, 1, accent);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), panelX + 15, textY, Theme.TEXT, false);
        Draw.text(g, font, "v" + mod.version(), panelX + 15 + font.width(title.copy().withStyle(ChatFormatting.BOLD)) + 5,
                textY, Theme.TEXT_MUTED);

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

    private Object renderSidebar(GuiGraphics g, int mouseX, int mouseY, float delta, Object hovered) {
        int y = panelY + 8;
        int accent = Theme.accent();
        for (Category entry : Category.values()) {
            boolean selected = query.isEmpty() && entry == category;
            boolean over = isOverSidebarItem(mouseX, mouseY, y);
            float h = categoryHover.computeIfAbsent(entry, c -> new Anim(0)).update(over || selected ? 1 : 0, delta, 18);
            if (h > 0) {
                int fill = selected ? Theme.alpha(accent, 0.16F) : Theme.alpha(0xFFFFFFFF, 0.05F * h);
                Draw.rect(g, panelX + 5, y, sidebarW - 10, SIDEBAR_ITEM - 2, 3, fill);
            }
            if (selected) {
                Draw.rect(g, panelX + 5, y + 4, 2, SIDEBAR_ITEM - 10, 1, accent);
            }
            int textColor = selected ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h);
            Draw.text(g, font, entry.icon(), panelX + 12, y + 5, selected ? accent : Theme.TEXT_MUTED);
            int count = enabledCount(entry);
            String countText = count > 0 ? Integer.toString(count) : "";
            int maxLabel = sidebarW - 38 - font.width(countText);
            Draw.text(g, font, Draw.ellipsize(font, entry.title().getString(), maxLabel), panelX + 24, y + 5, textColor);
            if (count > 0) {
                Draw.textRight(g, font, countText, panelX + sidebarW - 10, y + 5, Theme.alpha(accent, selected ? 1 : 0.7F));
            }
            y += SIDEBAR_ITEM;
        }

        int profilesY = panelY + panelH - SIDEBAR_ITEM - 6;
        g.fill(panelX + 8, profilesY - 5, panelX + sidebarW - 8, profilesY - 4, Theme.DIVIDER);
        boolean over = isOverSidebarItem(mouseX, mouseY, profilesY);
        float h = profilesHover.update(over ? 1 : 0, delta, 18);
        if (h > 0) {
            Draw.rect(g, panelX + 5, profilesY, sidebarW - 10, SIDEBAR_ITEM - 2, 3, Theme.alpha(0xFFFFFFFF, 0.05F * h));
        }
        Draw.text(g, font, "☰", panelX + 12, profilesY + 5, Theme.mix(Theme.TEXT_MUTED, accent, h));
        Draw.text(g, font, Draw.ellipsize(font, I18n.get("multiclicker.gui.profiles"), sidebarW - 34),
                panelX + 24, profilesY + 5, Theme.mix(Theme.TEXT_DIM, Theme.TEXT, h));
        if (over) {
            hovered = "profiles";
        }
        return hovered;
    }

    private boolean isOverSidebarItem(double mouseX, double mouseY, int itemY) {
        return mouseX >= panelX + 5 && mouseX < panelX + sidebarW - 5 && mouseY >= itemY && mouseY < itemY + SIDEBAR_ITEM - 2;
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

    private Object renderContent(GuiGraphics g, int mouseX, int mouseY, float delta, Object hovered) {
        scroll += (scrollTarget - scroll) * Math.min(1, delta * 16);
        if (Math.abs(scrollTarget - scroll) < 0.5F) {
            scroll = scrollTarget;
        }
        boolean mouseInView = mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH;
        int cardX = viewX + CARD_MARGIN;
        int cardW = viewW - CARD_MARGIN * 2 - 4;

        g.enableScissor(viewX + 1, viewY, viewX + viewW - 1, viewY + viewH);
        int y = viewY + CARD_MARGIN - Math.round(scroll);
        for (Card card : cards) {
            card.y = y;
            int height = CARD_HEADER;
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
        contentHeight = y + Math.round(scroll) - viewY + CARD_MARGIN - CARD_GAP;
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

    private Object drawCard(GuiGraphics g, Card card, int x, int w, int mouseX, int mouseY, boolean mouseInView, float delta,
                            Object hovered) {
        Module module = card.module;
        boolean enabled = module.isEnabled();
        int accent = Theme.accent();
        Draw.box(g, x, card.y, w, card.height, 4, Theme.CARD, Theme.CARD_BORDER);
        if (card.toggle != null) {
            Draw.rect(g, x + 1, card.y + 8, 2, 16, 1, enabled ? accent : Theme.CONTROL_HOVER);
        }

        int textRight = x + w - 10 - (card.toggle != null ? 30 : 0);
        g.drawString(font, Component.literal(Draw.ellipsize(font, module.name().getString(), textRight - x - 12))
                .withStyle(ChatFormatting.BOLD), x + 10, card.y + 7, enabled ? Theme.TEXT : Theme.TEXT_DIM, false);
        Draw.text(g, font, Draw.ellipsize(font, module.description().getString(), textRight - x - 10), x + 10, card.y + 19,
                Theme.TEXT_MUTED);
        if (card.toggle != null) {
            card.toggle.render(g, font, x + w - 32, card.y + 4, 24, mouseX, mouseY, delta);
        }
        if (mouseInView && mouseX >= x && mouseX < textRight && mouseY >= card.y && mouseY < card.y + CARD_HEADER) {
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
            if (overRow) {
                g.fill(x + 1, row.y, x + w - 1, row.y + ROW, Theme.ROW_HOVER);
                hovered = setting;
            }
            SettingControl control = control(setting);
            int controlW = control.width(font);
            int controlX = x + w - 10 - controlW;
            String label = Draw.ellipsize(font, setting.name().getString(), controlX - x - 20);
            Draw.text(g, font, label, x + 10, row.y + 5, enabled ? Theme.TEXT : Theme.TEXT_DIM);
            control.render(g, font, controlX, row.y, ROW, mouseX, mouseY, delta);
        }
        return hovered;
    }

    private void renderFooter(GuiGraphics g) {
        int y = panelY + panelH - FOOTER + 4;
        g.fill(viewX + 1, panelY + panelH - FOOTER, panelX + panelW - 1, panelY + panelH - FOOTER + 1, Theme.DIVIDER);
        String hint = I18n.get("multiclicker.gui.hint", mod.toggleKey.getTranslatedKeyMessage().getString());
        Draw.textCentered(g, font, Draw.ellipsize(font, hint, viewW - 16), viewX + viewW / 2, y, Theme.TEXT_MUTED);
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY, @Nullable Object hovered) {
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
        } else if (hovered instanceof Module module) {
            text = module.description();
        } else if ("profiles".equals(hovered)) {
            text = Component.translatable("multiclicker.gui.profiles.desc");
        }
        if (text != null) {
            g.setTooltipForNextFrame(font, font.split(text, 220), mouseX, mouseY);
        }
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - viewH);
    }

    // --- Input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean overSearch = mouseX >= searchX && mouseX < searchX + searchW && mouseY >= panelY + 7 && mouseY < panelY + 23;
        if (overSearch) {
            setFocused(search);
            search.setFocused(true);
            if (button == 1) {
                search.setValue("");
            }
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        search.setFocused(false);
        setFocused(null);

        if (isOverMaster(mouseX, mouseY) && button == 0 && minecraft.player != null) {
            mod.setActive(!mod.isActive(), null);
            return true;
        }
        int y = panelY + 8;
        for (Category entry : Category.values()) {
            if (isOverSidebarItem(mouseX, mouseY, y)) {
                category = lastCategory = entry;
                search.setValue("");
                query = "";
                rebuild();
                playClick();
                return true;
            }
            y += SIDEBAR_ITEM;
        }
        if (isOverSidebarItem(mouseX, mouseY, panelY + panelH - SIDEBAR_ITEM - 6)) {
            playClick();
            minecraft.setScreen(new ProfilesScreen(this));
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
                BoolSetting enabled = card.module.enabledSetting();
                boolean overHeader = mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= card.y && mouseY < card.y + CARD_HEADER;
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        if (dragging != null && dragging.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        if (dragging != null) {
            dragging.mouseReleased();
            dragging = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hoverKey instanceof Setting<?> setting && control(setting).mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (mouseX >= viewX && mouseX < viewX + viewW && mouseY >= viewY && mouseY < viewY + viewH) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 28, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            setFocused(search);
            search.setFocused(true);
            return true;
        }
        if (!search.isFocused() && hoverKey instanceof Setting<?> setting
                && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            return control(setting).adjust(keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Typing anywhere starts a search.
        if (!search.isFocused() && Character.isLetterOrDigit(codePoint)) {
            setFocused(search);
            search.setFocused(true);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void scrollFromMouse(double mouseY) {
        float t = (float) ((mouseY - viewY) / viewH);
        scrollTarget = scroll = Mth.clamp(t * contentHeight - viewH / 2.0F, 0, maxScroll());
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.35F));
    }
}
