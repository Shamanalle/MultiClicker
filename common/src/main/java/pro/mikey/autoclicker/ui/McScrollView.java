package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;

/**
 * McScrollView — scrollable container for any McWidget.
 * Features: smooth scroll, scrollbar with accent glow, fade edges.
 */
public class McScrollView extends McWidget {

    private final List<McWidget> children = new ArrayList<>();
    private double scroll, targetScroll;
    private boolean barDrag;
    private double dragStart, dragScrollStart;

    private static final int GAP = 12;
    private static final int PAD = 10;
    private static final int BAR_W = 4;
    private static final int FADE = 14;

    public McScrollView(int x, int y, int w, int h) {
        super(x, y, w, h, "");
    }

    public void clear() { children.clear(); scroll = targetScroll = 0; }

    public void add(McWidget w) { children.add(w); }

    public List<McWidget> getChildren() { return children; }

    private int contentH() {
        int h = PAD;
        for (McWidget c : children) h += c.scrollHeight() + GAP;
        return h;
    }

    private double maxScroll() { return Math.max(0, contentH() - h + PAD); }

    private void clamp() {
        double m = maxScroll();
        targetScroll = Math.max(0, Math.min(m, targetScroll));
        scroll = Math.max(0, Math.min(m, scroll));
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        // Smooth scroll
        float f = 1f - (float) Math.pow(0.05, dt / 3.0);
        scroll += (targetScroll - scroll) * f;
        if (Math.abs(scroll - targetScroll) < 0.5) scroll = targetScroll;
        clamp();

        // Background
        fill(g, x, y, x + w, y + h, 0xC00A0A12);

        // Scissor
        int cr = x + w - BAR_W - 2;
        g.enableScissor(x, y, cr, y + h);

        // Layout & render (centered)
        int cy = y + PAD - (int) scroll;
        for (McWidget c : children) {
            int cx = x + (w - BAR_W - 2 - c.getW()) / 2;
            if (c instanceof McSection sec) sec.layout(cx, cy);
            else c.setPos(cx, cy);
            int ch = c.scrollHeight();
            if (cy + ch > y && cy < y + h) c.render(g, mx, my, dt);
            cy += ch + GAP;
        }

        g.disableScissor();

        // Fade edges
        if (scroll > 2) {
            for (int b = 0; b < 4; b++) {
                int a = (int)((1.0 - (float) b / 4) * 160);
                fill(g, x, y + b * (FADE / 4), cr, y + (b + 1) * (FADE / 4), (a << 24) | 0x000A0A12);
            }
        }
        double ms = maxScroll();
        if (ms > 0 && scroll < ms - 2) {
            for (int b = 0; b < 4; b++) {
                int a = (int)((float)(b + 1) / 4 * 160);
                fill(g, x, y + h - FADE + b * (FADE / 4), cr, y + h - FADE + (b + 1) * (FADE / 4), (a << 24) | 0x000A0A12);
            }
        }

        // Scrollbar
        if (ms > 0) {
            int ch = contentH();
            int aH = h - 8;
            int bH = Math.max(20, (int)((double) h / ch * aH));
            int bY = y + 4 + (int)((scroll / ms) * (aH - bH));
            int bX = x + w - BAR_W - 1;

            fill(g, bX, y + 4, bX + BAR_W, y + h - 4, 0x10FFFFFF);
            boolean bHov = mx >= bX - 2 && mx <= bX + BAR_W + 2 && my >= bY && my <= bY + bH;
            if (barDrag || bHov) {
                fill(g, bX - 1, bY - 1, bX + BAR_W + 1, bY + bH + 1,
                        (0x18 << 24) | (accent() & 0x00FFFFFF));
            }
            int tc = barDrag ? 0x70FFFFFF : bHov ? 0x50FFFFFF : 0x28FFFFFF;
            fill(g, bX, bY, bX + BAR_W, bY + bH, tc);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dh, double dv) {
        if (!isOver(mx, my)) return false;
        targetScroll -= dv * 36;
        clamp();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible || !isOver(mx, my)) return false;
        // Scrollbar drag
        if (btn == 0 && mx >= x + w - BAR_W - 2) {
            barDrag = true;
            dragStart = my;
            dragScrollStart = scroll;
            return true;
        }
        for (McWidget c : children) if (c.mouseClicked(mx, my, btn)) return true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (barDrag && btn == 0) { barDrag = false; return true; }
        for (McWidget c : children) if (c.mouseReleased(mx, my, btn)) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (barDrag) {
            double ms = maxScroll();
            if (ms > 0) {
                int aH = h - 8;
                int ch = contentH();
                int bH = Math.max(20, (int)((double) h / ch * aH));
                targetScroll = dragScrollStart + (my - dragStart) / (aH - bH) * ms;
                clamp();
            }
            return true;
        }
        for (McWidget c : children) if (c.mouseDragged(mx, my, btn, dx, dy)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (McWidget c : children) if (c.keyPressed(key, scan, mods)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        for (McWidget c : children) if (c.charTyped(ch, mods)) return true;
        return false;
    }
}
