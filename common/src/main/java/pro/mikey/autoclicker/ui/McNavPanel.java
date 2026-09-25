package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pro.mikey.autoclicker.core.Setting;

import java.util.*;
import java.util.function.Consumer;

/**
 * McNavPanel — sidebar navigation for OptionsScreen.
 * Shows module categories with collapsible groups,
 * status dots (● enabled / ○ disabled), and selection highlight.
 */
public class McNavPanel extends McWidget {

    // ── Data structures ──
    public record NavItem(String id, String label, Setting.BooleanSetting status) {}
    public record NavCategory(String icon, String label, int color, List<NavItem> items) {}

    private final List<NavCategory> categories = new ArrayList<>();
    private final Set<Integer> collapsed = new HashSet<>();
    private String selectedId = "";
    private Consumer<String> onSelect;

    // ── Scroll state ──
    private double scroll, targetScroll;

    // ── Layout constants ──
    private static final int CAT_H = 22;   // category header height
    private static final int ITEM_H = 18;  // module item height
    private static final int INDENT = 14;  // item indent from left
    private static final int DOT_R = 3;    // status dot radius
    private static final int PAD_TOP = 4;

    public McNavPanel(int x, int y, int w, int h) {
        super(x, y, w, h, "nav");
    }

    public void setOnSelect(Consumer<String> cb) { this.onSelect = cb; }
    public String getSelectedId() { return selectedId; }
    public void setSelectedId(String id) { this.selectedId = id; }

    public void addCategory(NavCategory cat) {
        categories.add(cat);
    }

    public void clearCategories() {
        categories.clear();
        collapsed.clear();
    }

    /** Filter items by search query, returning true if any visible. */
    public boolean applyFilter(String query) {
        // No filtering needed — OptionsScreen handles selection
        return true;
    }

    /** Get total content height for scrolling. */
    private int contentHeight() {
        int total = PAD_TOP;
        for (int ci = 0; ci < categories.size(); ci++) {
            NavCategory cat = categories.get(ci);
            total += CAT_H;
            if (!collapsed.contains(ci)) {
                total += cat.items().size() * ITEM_H;
            }
        }
        return total + 4;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight() - h);
    }

    private void clampScroll() {
        double m = maxScroll();
        targetScroll = Math.max(0, Math.min(m, targetScroll));
        scroll = Math.max(0, Math.min(m, scroll));
    }

    // ══════════════════════════════════════════════════
    // Rendering
    // ══════════════════════════════════════════════════

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        // Smooth scroll
        float f = 1f - (float) Math.pow(0.05, dt / 3.0);
        scroll += (targetScroll - scroll) * f;
        if (Math.abs(scroll - targetScroll) < 0.5) scroll = targetScroll;
        clampScroll();

        // Background
        fill(g, x, y, x + w, y + h, 0xE00A0A14);

        // Right border
        fill(g, x + w - 1, y, x + w, y + h, C_BORDER);

        // Scissor for content
        g.enableScissor(x, y, x + w - 1, y + h);

        int cy = y + PAD_TOP - (int) scroll;

        for (int ci = 0; ci < categories.size(); ci++) {
            NavCategory cat = categories.get(ci);
            boolean catCol = collapsed.contains(ci);

            // === Category header ===
            boolean catHov = mx >= x && mx < x + w - 1 && my >= cy && my < cy + CAT_H;
            if (catHov) fill(g, x, cy, x + w - 1, cy + CAT_H, 0x0CFFFFFF);

            // Category accent bar
            fill(g, x, cy + 3, x + 2, cy + CAT_H - 3, cat.color());

            // Category label (icon + text)
            String catLabel = cat.icon() + " " + cat.label();
            txts(g, catLabel, x + 6, cy + (CAT_H - 9) / 2, 0xFFCCCCDD);

            // Collapse indicator
            txt(g, catCol ? "+" : "−", x + w - 14, cy + (CAT_H - 9) / 2, 0xFF555577);

            // Separator under header
            fill(g, x + 4, cy + CAT_H - 1, x + w - 4, cy + CAT_H, 0x0AFFFFFF);

            cy += CAT_H;

            // === Items ===
            if (!catCol) {
                for (NavItem item : cat.items()) {
                    boolean selected = item.id().equals(selectedId);
                    boolean itemHov = mx >= x && mx < x + w - 1 && my >= cy && my < cy + ITEM_H;

                    // Selection highlight
                    if (selected) {
                        fill(g, x, cy, x + w - 1, cy + ITEM_H,
                            (0x18 << 24) | (cat.color() & 0x00FFFFFF));
                        fill(g, x, cy, x + 2, cy + ITEM_H, accent());
                    } else if (itemHov) {
                        fill(g, x, cy, x + w - 1, cy + ITEM_H, 0x0CFFFFFF);
                    }

                    // Status dot
                    boolean isOn = item.status() != null && item.status().get();
                    int dotColor = isOn ? C_ON : C_OFF;
                    int dotCx = x + INDENT;
                    int dotCy = cy + ITEM_H / 2;
                    circle(g, dotCx, dotCy, DOT_R, dotColor);
                    if (isOn) {
                        // Subtle glow
                        circle(g, dotCx, dotCy, DOT_R + 2,
                            (0x18 << 24) | (C_ON & 0x00FFFFFF));
                    }

                    // Label
                    int labelColor = selected ? C_TEXT : (itemHov ? 0xFFBBBBDD : C_TEXT_DIM);
                    txt(g, item.label(), x + INDENT + DOT_R + 6, cy + (ITEM_H - 9) / 2, labelColor);

                    cy += ITEM_H;
                }
            }
        }

        g.disableScissor();
    }

    // ══════════════════════════════════════════════════
    // Input
    // ══════════════════════════════════════════════════

    @Override
    protected boolean onClick(double mx, double my, int btn) {
        if (btn != 0) return false;

        int cy = y + PAD_TOP - (int) scroll;

        for (int ci = 0; ci < categories.size(); ci++) {
            NavCategory cat = categories.get(ci);
            boolean catCol = collapsed.contains(ci);

            // Click on category header → toggle collapse
            if (my >= cy && my < cy + CAT_H) {
                if (catCol) collapsed.remove(ci);
                else collapsed.add(ci);
                return true;
            }
            cy += CAT_H;

            // Click on items
            if (!catCol) {
                for (NavItem item : cat.items()) {
                    if (my >= cy && my < cy + ITEM_H) {
                        selectedId = item.id();
                        if (onSelect != null) onSelect.accept(item.id());
                        return true;
                    }
                    cy += ITEM_H;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dh, double dv) {
        if (!isOver(mx, my)) return false;
        targetScroll -= dv * 24;
        clampScroll();
        return true;
    }
}
