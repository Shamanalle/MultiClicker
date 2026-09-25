package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pro.mikey.autoclicker.core.Setting;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * McSection v5 — polished settings card.
 *   - Master toggle in header (no ON/OFF text — toggle speaks for itself)
 *   - Thinner accent bar (3px)
 *   - More breathing room (CP=10, RH=38)
 *   - Subtle └ connectors for dependent settings
 */
public class McSection extends McWidget {

    private final String title;
    private final int accentColor;
    private final Setting.BooleanSetting master;

    private final List<Entry> entries = new ArrayList<>();
    private final List<McWidget> allWidgets = new ArrayList<>();

    private boolean collapsed;
    private float colAnim = 1f;
    private float masterAnim;
    private BooleanSupplier dimCond;
    private String dimNote;
    private Runnable onReset;

    private static final int HH = 26;     // header height
    private static final int RH = 38;     // row height — more breathing room
    private static final int BW = 3;      // accent bar width — thinner
    private static final int CP = 10;     // card padding — more air
    private static final int INDENT = 14; // indent for dependent settings

    public McSection(int x, int y, int w, String title, int accentColor,
                     Setting.BooleanSetting master) {
        super(x, y, w, 0, title);
        this.title = title;
        this.accentColor = accentColor;
        this.master = master;
        this.masterAnim = (master != null && master.get()) ? 1f : 0f;
    }

    public McSection add(Setting<?> s) {
        entries.add(new Entry(s, null, null));
        return this;
    }

    public McSection addIf(Setting<?> s, Setting.BooleanSetting gate, boolean expected) {
        entries.add(new Entry(s, () -> gate.get() == expected, gate));
        return this;
    }

    public <E extends Enum<E>> McSection addIf(Setting<?> s, Setting.EnumSetting<E> gate, E expected) {
        entries.add(new Entry(s, () -> gate.get() == expected, gate));
        return this;
    }

    public McSection withDim(BooleanSupplier c, String n) { dimCond = c; dimNote = n; return this; }
    public McSection withReset(Runnable r) { onReset = r; return this; }

    public McSection build() {
        allWidgets.clear();
        int cw = w - BW - 8 - CP * 2;
        for (Entry e : entries) {
            McWidget wid = createWidget(e.setting, cw, e.gate != null);
            if (wid != null) {
                e.widget = wid;
                allWidgets.add(wid);
            }
        }
        return this;
    }

    private McWidget createWidget(Setting<?> s, int cw, boolean indented) {
        int iw = indented ? cw - INDENT : cw;
        McWidget wid = null;
        if (s instanceof Setting.BooleanSetting bs)
            wid = new McToggle(0, 0, iw, RH, bs.getDisplayName(), bs.get(), bs::set);
        else if (s instanceof Setting.IntSetting is)
            wid = McSlider.ofInt(0, 0, iw, RH, is.getDisplayName(), is.get(), is.getMin(), is.getMax(), "", is::set);
        else if (s instanceof Setting.FloatSetting fs)
            wid = new McSlider(0, 0, iw, RH, fs.getDisplayName(), fs.get(), fs.getMin(), fs.getMax(), 0.01, "", n -> fs.set(n.doubleValue()));
        else if (s instanceof Setting.EnumSetting<?> es)
            wid = new McDropdown(0, 0, iw, RH, es);
        else if (s instanceof Setting.StringListSetting sls)
            wid = new McStringListEditor(0, 0, iw, sls);
        if (wid != null) {
            String desc = s.getDescription();
            if (desc != null && !desc.isEmpty()) wid.setSubtitle(desc);
        }
        return wid;
    }

    @Override
    public int scrollHeight() {
        int t = HH + CP;
        if (!collapsed) {
            for (Entry e : entries) {
                if (e.widget == null) continue;
                if (e.condition != null && !e.condition.getAsBoolean()) continue;
                t += e.widget.scrollHeight() + 2;
            }
            t += CP;
            if (dimCond != null && dimCond.getAsBoolean() && dimNote != null) t += 14;
        }
        return t;
    }

    public void layout(int sx, int sy) {
        x = sx; y = sy;
        boolean masterOff = master != null && !master.get();
        boolean shouldDim = (dimCond != null && dimCond.getAsBoolean()) || masterOff;
        int cy = sy + HH;
        if (shouldDim && dimNote != null && !collapsed && dimCond != null && dimCond.getAsBoolean()) cy += 14;
        cy += CP;

        for (Entry e : entries) {
            if (e.widget == null) continue;
            boolean vis = !collapsed && (e.condition == null || e.condition.getAsBoolean());
            e.widget.setVisible(vis);
            e.widget.setDimmed(shouldDim);
            if (vis) {
                int indent = (e.gate != null) ? INDENT : 0;
                e.widget.setPos(sx + BW + 6 + CP + indent, cy);
                cy += e.widget.scrollHeight() + 2;
            }
        }
        h = scrollHeight();
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        colAnim = spring(colAnim, collapsed ? 0f : 1f, 0.25f, dt);
        boolean masterOff = master != null && !master.get();
        boolean shouldDim = (dimCond != null && dimCond.getAsBoolean()) || masterOff;
        if (master != null) masterAnim = spring(masterAnim, master.get() ? 1f : 0f, 0.2f, dt);

        // Card background — brighter when active, darker when dimmed
        shadow(g, x, y, w, h, 2);
        int cardBg = shouldDim ? 0xC00A0A15 : 0xF015152A;
        int border = shouldDim ? 0xFF1E1E35 : 0xFF282850;
        panel(g, x, y, w, h, cardBg, border);
        // Subtle top highlight on active cards
        if (!shouldDim) fill(g, x + 1, y, x + w - 1, y + 1, 0x0CFFFFFF);

        // Accent bar — thinner, elegant
        int barColor = shouldDim ? lerp(accentColor, 0xFF333344, 0.7f) : accentColor;
        fill(g, x + 1, y + 2, x + 1 + BW, y + h - 2, barColor);

        // Header
        boolean headHov = mx >= x && mx < x + w && my >= y && my < y + HH;
        if (headHov) fill(g, x + BW + 1, y + 1, x + w - 1, y + HH, 0x0CFFFFFF);
        int titleColor = shouldDim ? C_TEXT_DIM : C_TEXT;
        txts(g, title, x + BW + 10, y + 8, titleColor);

        // Master toggle — minimal, no ON/OFF text
        if (master != null) {
            int trkW = 28, trkH = 12;
            int trkX = x + w - trkW - 12;
            int trkY = y + (HH - trkH) / 2;
            // Track
            int trackC = lerp(0xFF222240, 0xFF1A3A28, masterAnim);
            pill(g, trkX, trkY, trkW, trkH, trackC);
            int brd = lerp(0xFF333350, 0xFF2A6A3A, masterAnim);
            fill(g, trkX + trkH / 2, trkY, trkX + trkW - trkH / 2, trkY + 1, brd);
            fill(g, trkX + trkH / 2, trkY + trkH - 1, trkX + trkW - trkH / 2, trkY + trkH, brd);
            // Thumb
            int thumbR = 5;
            int thumbX = trkX + thumbR + (int)((trkW - thumbR * 2) * masterAnim);
            int thumbY = trkY + trkH / 2;
            if (masterAnim > 0.3f) circle(g, thumbX, thumbY, thumbR + 3, ((int)((masterAnim - 0.3f) * 40)) << 24 | (C_ON & 0x00FFFFFF));
            circle(g, thumbX, thumbY, thumbR, lerp(0xFFAABBCC, 0xFFFFFFFF, masterAnim));
            if (masterAnim > 0.5f) circle(g, thumbX, thumbY, 2, ((int)((masterAnim - 0.5f) * 2 * 255)) << 24 | (C_ON & 0x00FFFFFF));
        }

        // Reset button — to the left of master toggle
        if (onReset != null && !collapsed) {
            int rx = master != null ? x + w - 50 : x + w - 16;
            boolean rh = mx >= rx - 4 && mx < rx + 10 && my >= y + 2 && my < y + HH - 2;
            txt(g, "R", rx, y + 8, rh ? C_WARN : 0xFF444466);
        }

        // Collapse arrow (only for non-master sections)
        if (master == null) {
            txt(g, collapsed ? "+" : "-", x + w - 14, y + 8, 0xFF555577);
        }

        // Header separator
        if (!collapsed) fill(g, x + BW + 2, y + HH - 1, x + w - 2, y + HH, (0x14 << 24) | (accentColor & 0x00FFFFFF));

        // Dim note
        if (dimCond != null && dimCond.getAsBoolean() && dimNote != null && !collapsed) {
            fill(g, x + BW + 2, y + HH, x + w - 2, y + HH + 13, 0x14FFCC44);
            txt(g, "! " + dimNote, x + BW + 10, y + HH + 2, C_WARN);
        }

        // Children
        if (!collapsed) {
            int idx = 0;
            for (Entry e : entries) {
                if (e.widget == null || !e.widget.isVisible()) continue;
                // Alternating tint — very subtle
                if (idx % 2 == 1)
                    fill(g, e.widget.getX() - 2, e.widget.getY(), e.widget.getX() + e.widget.getW() + 2,
                            e.widget.getY() + e.widget.scrollHeight(), 0x06FFFFFF);
                // Indent connector └ — very subtle
                if (e.gate != null) {
                    int cx = e.widget.getX() - INDENT + 3;
                    int cy = e.widget.getY() + e.widget.scrollHeight() / 2;
                    fill(g, cx, e.widget.getY() + 2, cx + 1, cy, 0xFF222238);
                    fill(g, cx, cy, cx + INDENT - 6, cy + 1, 0xFF222238);
                }
                e.widget.render(g, mx, my, dt);
                idx++;
            }
        }
    }

    @Override
    protected boolean onClick(double mx, double my, int btn) {
        if (btn != 0) return false;

        if (my >= y && my < y + HH) {
            if (master != null) {
                int trkW = 28;
                int trkX = x + w - trkW - 12;
                int toggleEnd = x + w - 4;
                if (mx >= trkX && mx < toggleEnd) {
                    master.set(!master.get());
                    return true;
                }
            }
            if (onReset != null && !collapsed) {
                int rx = master != null ? x + w - 50 : x + w - 16;
                if (mx >= rx - 4 && mx < rx + 10) {
                    for (Entry e : entries) if (e.setting != null) e.setting.reset();
                    if (master != null) master.reset();
                    onReset.run();
                    rebuildWidgets();
                    return true;
                }
            }
            if (master == null) {
                collapsed = !collapsed;
                return true;
            }
            return false;
        }

        if (!collapsed) {
            for (Entry e : entries) {
                if (e.widget != null && e.widget.isVisible() && e.widget.mouseClicked(mx, my, btn))
                    return true;
            }
        }
        return false;
    }

    private void rebuildWidgets() {
        int cw = w - BW - 8 - CP * 2;
        for (Entry e : entries) {
            if (e.setting == null) continue;
            McWidget wid = createWidget(e.setting, cw, e.gate != null);
            if (wid != null) e.widget = wid;
        }
        allWidgets.clear();
        for (Entry e : entries) if (e.widget != null) allWidgets.add(e.widget);
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) {
        if (!collapsed) for (Entry e : entries) if (e.widget != null && e.widget.isVisible() && e.widget.mouseReleased(mx, my, btn)) return true;
        return false;
    }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (!collapsed) for (Entry e : entries) if (e.widget != null && e.widget.isVisible() && e.widget.mouseDragged(mx, my, btn, dx, dy)) return true;
        return false;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (!collapsed) for (Entry e : entries) if (e.widget != null && e.widget.isVisible() && e.widget.keyPressed(key, scan, mods)) return true;
        return false;
    }
    @Override public boolean charTyped(char ch, int mods) {
        if (!collapsed) for (Entry e : entries) if (e.widget != null && e.widget.isVisible() && e.widget.charTyped(ch, mods)) return true;
        return false;
    }

    private static class Entry {
        final Setting<?> setting;
        final BooleanSupplier condition;
        final Setting<?> gate;
        McWidget widget;

        Entry(Setting<?> s, BooleanSupplier cond, Setting<?> gate) {
            this.setting = s; this.condition = cond; this.gate = gate;
        }
    }
}
