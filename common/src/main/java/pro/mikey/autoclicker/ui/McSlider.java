package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Consumer;

public class McSlider extends McWidget {

    private double value, min, max, step;
    private final Consumer<Number> onChange;
    private boolean dragging;
    private String suffix;
    private static final int TH = 4, VW = 34;

    public McSlider(int x, int y, int w, int h, String label,
                    double value, double min, double max, double step,
                    String suffix, Consumer<Number> onChange) {
        super(x, y, w, h, label);
        this.value = value; this.min = min; this.max = max;
        this.step = step; this.suffix = suffix; this.onChange = onChange;
    }

    public static McSlider ofInt(int x, int y, int w, int h, String label,
                                  int value, int min, int max, String suffix,
                                  Consumer<Integer> onChange) {
        return new McSlider(x, y, w, h, label, value, min, max, 1.0, suffix, n -> onChange.accept(n.intValue()));
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        if (hoverAnim > 0.01f) fill(g, x, y, x + w, y + h, (int)(hoverAnim * 10) << 24 | 0x00FFFFFF);

        int ly = subtitle != null ? y + 5 : y + (h - 9) / 2;
        txts(g, label, x + 8, ly, dimmed ? C_TEXT_DIM : C_TEXT);
        if (subtitle != null) drawSub(g, x + 8, ly);

        // Track area
        int tR = x + w - VW - 14, tL = x + w / 2 - 10, tW = tR - tL, tY = y + h / 2 - TH / 2;

        // Background track (thin line)
        fill(g, tL, tY, tR, tY + TH, 0xFF1A1A30);
        fill(g, tL, tY, tR, tY + 1, 0x08000000); // top shadow

        // Filled portion
        double norm = (max > min) ? (value - min) / (max - min) : 0;
        int fW = (int)(tW * norm);
        if (fW > 1) {
            int ac = dimmed ? lerp(accent(), 0xFF333344, 0.6f) : accent();
            fill(g, tL, tY, tL + fW, tY + TH, ac);
            fill(g, tL, tY, tL + fW, tY + 1, lerp(ac, 0xFFFFFFFF, 0.15f)); // highlight
        }

        // Thumb
        int thumbX = tL + fW, thumbCY = tY + TH / 2;
        boolean tHov = dragging || (Math.abs(mx - thumbX) < 8 && Math.abs(my - thumbCY) < 10);
        if (tHov && !dimmed) circle(g, thumbX, thumbCY, 8, (0x18 << 24) | (accent() & 0x00FFFFFF));
        int tColor = dimmed ? 0xFF777799 : accent();
        circle(g, thumbX, thumbCY, 5, tColor);
        circle(g, thumbX, thumbCY, 3, dimmed ? 0xFFAAAAAA : 0xFFFFFFFF);

        // Value pill
        String v = (step >= 1.0 ? String.valueOf((int)value) : String.format("%.1f", value)) + suffix;
        int pw = tw(v) + 8, px = tR + 8, py = y + (h - 14) / 2;
        panel(g, px, py, pw, 14, 0xFF131328, 0xFF252548);
        txt(g, v, px + 4, py + 3, dimmed ? C_TEXT_DIM : C_TEXT);
    }

    @Override protected boolean onClick(double mx, double my, int btn) {
        if (btn != 0 || dimmed) return false; dragging = true; updateValue(mx); return true;
    }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) { updateValue(mx); return true; } return false;
    }
    @Override public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging) { dragging = false; return true; } return false;
    }

    private void updateValue(double mx) {
        int tL = x + w/2-10, tR = x+w-VW-14, tW = tR-tL;
        double n = Math.max(0, Math.min(1, (mx-tL)/tW)), raw = min + n*(max-min);
        value = Math.round(raw/step)*step;
        value = Math.max(min, Math.min(max, value));
        onChange.accept(step >= 1.0 ? (int)value : (float)value);
    }
}
