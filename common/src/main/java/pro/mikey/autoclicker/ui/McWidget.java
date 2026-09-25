package pro.mikey.autoclicker.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import pro.mikey.autoclicker.AutoClicker;

/**
 * McWidget — base for all MultiClicker UI.
 * Uses MINECRAFT'S BUILT-IN FONT (no custom font).
 * Pure custom rendering via GuiGraphics blit.
 */
public abstract class McWidget implements Renderable, GuiEventListener, NarratableEntry {

    // ═══ Color Palette ═══
    public static final int C_BG        = 0xFF0D0D17;
    public static final int C_PANEL     = 0xFF13132A;
    public static final int C_PANEL_HI  = 0xFF1C1C38;
    public static final int C_BORDER    = 0xFF262648;
    public static final int C_TEXT      = 0xFFDDDDEE;
    public static final int C_TEXT_DIM  = 0xFF7777AA;
    public static final int C_TEXT_HINT = 0xFF555580;
    public static final int C_ON        = 0xFF55FF99;
    public static final int C_OFF       = 0xFF666688;
    public static final int C_DANGER    = 0xFFFF5566;
    public static final int C_WARN      = 0xFFFFCC44;

    // ═══ White pixel for rendering ═══
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("multiclicker", "textures/gui/white.png");

    // ═══ Fields ═══
    protected int x, y, w, h;
    protected boolean hovered, focused, visible = true, dimmed;
    protected String label, subtitle, tooltip;
    protected float hoverAnim;

    public McWidget(int x, int y, int w, int h, String label) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getW() { return w; }
    public int getH() { return h; }
    public void setPos(int x, int y) { this.x = x; this.y = y; }
    public void setSize(int w, int h) { this.w = w; this.h = h; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }
    public void setDimmed(boolean d) { dimmed = d; }
    public String getLabel() { return label; }
    public void setSubtitle(String s) { subtitle = s; }
    public int scrollHeight() { return h; }

    // ═══ Rendering ═══

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float dt) {
        if (!visible) return;
        hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        hoverAnim = spring(hoverAnim, hovered ? 1f : 0f, 0.3f, dt);
        draw(gfx, mx, my, dt);
        if (dimmed) fill(gfx, x, y, x + w, y + h, 0xA0000010);
    }

    protected abstract void draw(GuiGraphics gfx, int mx, int my, float dt);

    // ═══ Input ═══

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!visible || dimmed) return false;
        double mx = event.x(), my = event.y();
        if (mx >= x && mx < x + w && my >= y && my < y + h) return onClick(mx, my, event.button());
        return false;
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible || dimmed) return false;
        if (mx >= x && mx < x + w && my >= y && my < y + h) return onClick(mx, my, btn);
        return false;
    }

    protected boolean onClick(double mx, double my, int btn) { return false; }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return mouseReleased(event.x(), event.y(), event.button());
    }
    public boolean mouseReleased(double mx, double my, int btn) { return false; }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return mouseDragged(event.x(), event.y(), event.button(), dx, dy);
    }
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { return false; }

    @Override public boolean mouseScrolled(double mx, double my, double dh, double dv) { return false; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }
    public boolean keyPressed(int key, int scan, int mods) { return false; }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), event.modifiers());
    }
    public boolean charTyped(char ch, int mods) { return false; }
    @Override public boolean isFocused() { return focused; }
    @Override public void setFocused(boolean f) { focused = f; }
    @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput out) {}

    // ═══ Static Helpers ═══

    public static Font font() { return Minecraft.getInstance().font; }

    public static int accent() {
        AutoClicker ac = AutoClicker.getInstance();
        return ac != null ? ac.getAccentColor() : 0xFF5599FF;
    }

    /** Fill rect via 1x1 white blit. */
    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || ((color >> 24) & 0xFF) == 0) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, WHITE, x1, y1, 0f, 0f, x2 - x1, y2 - y1, x2 - x1, y2 - y1, color);
    }

    /** Draw text using Minecraft's built-in font. */
    public static void txt(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, false);
    }

    /** Draw text with drop shadow. */
    public static void txts(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, true);
    }

    /** Text width using Minecraft font. */
    public static int tw(String s) { return font().width(s); }

    /** Rounded panel with border (fake corners). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int bg, int brd) {
        fill(g, x + 2, y, x + w - 2, y + h, bg);
        fill(g, x, y + 2, x + 2, y + h - 2, bg);
        fill(g, x + w - 2, y + 2, x + w, y + h - 2, bg);
        fill(g, x + 1, y + 1, x + 2, y + 2, bg);
        fill(g, x + w - 2, y + 1, x + w - 1, y + 2, bg);
        fill(g, x + 1, y + h - 2, x + 2, y + h - 1, bg);
        fill(g, x + w - 2, y + h - 2, x + w - 1, y + h - 1, bg);
        fill(g, x + 2, y, x + w - 2, y + 1, brd);
        fill(g, x + 2, y + h - 1, x + w - 2, y + h, brd);
        fill(g, x, y + 2, x + 1, y + h - 2, brd);
        fill(g, x + w - 1, y + 2, x + w, y + h - 2, brd);
        fill(g, x + 1, y + 1, x + 2, y + 2, brd);
        fill(g, x + w - 2, y + 1, x + w - 1, y + 2, brd);
        fill(g, x + 1, y + h - 2, x + 2, y + h - 1, brd);
        fill(g, x + w - 2, y + h - 2, x + w - 1, y + h - 1, brd);
    }

    public static void gradV(GuiGraphics g, int x1, int y1, int x2, int y2, int cT, int cB) {
        int h = y2 - y1;
        if (h <= 0) return;
        int seg = Math.min(h, 16);
        int sh = Math.max(1, h / seg);
        for (int i = 0; i < seg; i++) {
            float t = (float) i / Math.max(1, seg - 1);
            int sy = y1 + i * sh;
            fill(g, x1, sy, x2, i == seg - 1 ? y2 : sy + sh, lerp(cT, cB, t));
        }
    }

    public static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            fill(g, cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    public static void pill(GuiGraphics g, int x, int y, int w, int h, int color) {
        int r = h / 2;
        if (r <= 0) { fill(g, x, y, x + w, y + h, color); return; }
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            fill(g, x + r - dx, y + r + dy, x + w - r + dx + 1, y + r + dy + 1, color);
        }
    }

    public static void shadow(GuiGraphics g, int x, int y, int w, int h, int layers) {
        for (int i = layers; i >= 1; i--)
            fill(g, x + i, y + i, x + w + i, y + h + i, (5 * i) << 24);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int)(((c1>>24)&0xFF) + (((c2>>24)&0xFF) - ((c1>>24)&0xFF)) * t);
        int r = (int)(((c1>>16)&0xFF) + (((c2>>16)&0xFF) - ((c1>>16)&0xFF)) * t);
        int g = (int)(((c1>>8)&0xFF) + (((c2>>8)&0xFF) - ((c1>>8)&0xFF)) * t);
        int b = (int)((c1&0xFF) + ((c2&0xFF) - (c1&0xFF)) * t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    public static float spring(float cur, float tgt, float spd, float dt) {
        return cur + (tgt - cur) * Math.min(1f, spd * dt * 20f);
    }

    /** Draw subtitle below label. */
    protected void drawSub(GuiGraphics g, int lx, int ly) {
        if (subtitle != null && !subtitle.isEmpty()) {
            String s = subtitle;
            int maxW = w - 16;
            if (maxW < tw("...")) {
                // Panel too narrow for any subtitle
            } else if (tw(s) > maxW) {
                while (tw(s + "...") > maxW && s.length() > 4) s = s.substring(0, s.length() - 1);
                s += "...";
            }
            txt(g, s, lx, ly + 12, C_TEXT_HINT);
        }
    }

    public boolean isOver(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
