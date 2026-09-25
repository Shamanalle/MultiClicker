package pro.mikey.autoclicker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.ui.McWidget;
import java.util.ArrayList;
import java.util.List;

public class HelpScreen extends Screen {

    private final Screen parent;
    private double scroll, tgtScroll;
    private List<Entry> entries;
    private static final int HH = 32, FH = 30, RH = 18, SH = 26, SP = 10;

    protected HelpScreen(Screen parent) { super(Component.empty()); this.parent = parent; }

    @Override protected void init() {
        entries = new ArrayList<>();
        sec("ОСНОВЫ");
        row("I", "Вкл/выкл MultiClicker");
        row("O", "Открыть настройки");
        row("Тики", "1 тик = 50мс, 20 тиков = 1с");
        row("Вкладки", "Кликер / Авто / Бой / Визуал");
        space();
        for (Module m : AutoClicker.getInstance().getModuleManager().getModules()) {
            List<Setting<?>> ss = m.getSettings();
            if (ss.isEmpty()) continue;
            sec(m.getDisplayName().toUpperCase());
            for (Setting<?> s : ss) {
                String d = s.getDescription();
                if (d == null || d.isEmpty()) {
                    if (s instanceof Setting.BooleanSetting) d = "Вкл/выкл";
                    else if (s instanceof Setting.IntSetting is) d = is.getMin() + " - " + is.getMax();
                    else if (s instanceof Setting.FloatSetting fs) d = fs.getMin() + " - " + fs.getMax();
                    else if (s instanceof Setting.EnumSetting<?>) d = "Выбор";
                    else d = "";
                }
                row(s.getDisplayName(), d);
            }
            space();
        }
        sec("КЛАВИШИ");
        row("I", "Вкл/выкл"); row("O", "Настройки"); row("ESC", "Закрыть");
    }

    private void sec(String t) { entries.add(new Entry(t, "", true)); }
    private void row(String l, String d) { entries.add(new Entry(l, d, false)); }
    private void space() { entries.add(new Entry("", "", false)); }

    private int eH(Entry e) { return e.header ? SH : e.label.isEmpty() ? SP : RH; }
    private int totH() { int h = 0; for (Entry e : entries) h += eH(e); return h; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        McWidget.fill(g, 0, 0, width, height, 0xF0080812);
        int ac = McWidget.accent();

        McWidget.gradV(g, 0, 0, width, HH, 0xFF111128, 0xFF0D0D20);
        McWidget.fill(g, 0, HH - 1, width, HH, (0x22 << 24) | (ac & 0x00FFFFFF));
        g.drawString(font, "Мануал MultiClicker", 12, (HH - 9) / 2, 0xFFE2E2EE, true);

        int total = 0;
        for (Module m : AutoClicker.getInstance().getModuleManager().getModules()) total += m.getSettings().size();
        String badge = total + " настроек";
        g.drawString(font, badge, width - font.width(badge) - 12, (HH - 9) / 2, 0xFF8888BB, false);

        int topY = HH, botY = height - FH, visH = botY - topY;
        int cw = Math.min(width - 40, 560), lx = (width - cw) / 2;

        scroll += (tgtScroll - scroll) * 0.3;
        int th = totH(), maxS = Math.max(0, th - visH + 12);
        tgtScroll = Math.max(0, Math.min(maxS, tgtScroll));

        g.enableScissor(0, topY, width, botY);
        int y = topY + 8 - (int)scroll;
        int ri = 0;
        for (Entry e : entries) {
            int eh = eH(e);
            if (y + eh > topY - 30 && y < botY + 30) {
                if (e.header) {
                    McWidget.shadow(g, lx, y, cw, SH, 2);
                    McWidget.panel(g, lx, y, cw, SH, 0xFF111128, 0xFF222248);
                    McWidget.fill(g, lx + 1, y + 3, lx + 6, y + SH - 3, ac);
                    g.drawString(font, e.label, lx + 12, y + 8, ac, true);
                } else if (!e.label.isEmpty()) {
                    if (ri % 2 == 0) McWidget.fill(g, lx + 6, y, lx + cw - 6, y + RH, 0x08FFFFFF);
                    g.drawString(font, e.label, lx + 18, y + 5, 0xFFEECC66, false);
                    int lw = font.width(e.label);
                    g.drawString(font, " -- ", lx + 18 + lw, y + 5, 0xFF333350, false);
                    g.drawString(font, e.desc, lx + 18 + lw + font.width(" -- "), y + 5, 0xFFAAAACC, false);
                    ri++;
                }
            }
            y += eh;
        }
        g.disableScissor();

        if (maxS > 0) {
            int bX = lx + cw + 6, aH = visH - 8;
            int bH = Math.max(18, (int)((double)visH / th * aH));
            int bY = topY + 4 + (int)((scroll / maxS) * (aH - bH));
            McWidget.fill(g, bX, topY + 4, bX + 4, botY - 4, 0x10FFFFFF);
            McWidget.fill(g, bX, bY, bX + 4, bY + bH, 0x30FFFFFF);
        }

        McWidget.fill(g, 0, botY, width, height, 0xFF0A0A14);
        McWidget.fill(g, 0, botY, width, botY + 1, 0x18FFFFFF);
        String back = "< Назад";
        int bw = font.width(back) + 16, bx = width / 2 - bw / 2, by = botY + (FH - 16) / 2;
        boolean bh = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
        McWidget.panel(g, bx, by, bw, 16, bh ? 0xFF1C1C38 : 0xFF111128, bh ? ac : 0xFF2A2A50);
        g.drawString(font, back, bx + 8, by + 4, bh ? 0xFFE2E2EE : 0xFF8888BB, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        int botY = height - FH;
        String back = "< Назад";
        int bw = font.width(back) + 16, bx = width / 2 - bw / 2, by = botY + (FH - 16) / 2;
        if (event.x() >= bx && event.x() < bx + bw && event.y() >= by && event.y() < by + 16) { minecraft.setScreen(parent); return true; }
        return super.mouseClicked(event, bl);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dh, double dv) { tgtScroll -= dv * 30; return true; }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }

    private static class Entry {
        final String label, desc; final boolean header;
        Entry(String l, String d, boolean h) { label = l; desc = d; header = h; }
    }
}
