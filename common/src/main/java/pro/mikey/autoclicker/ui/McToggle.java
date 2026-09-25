package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Consumer;

public class McToggle extends McWidget {

    private boolean value;
    private final Consumer<Boolean> onChange;
    private float anim;

    private static final int TW = 28, TH = 12, TR = 5;

    public McToggle(int x, int y, int w, int h, String label, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, w, h, label);
        this.value = initial;
        this.onChange = onChange;
        this.anim = initial ? 1f : 0f;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        anim = spring(anim, value ? 1f : 0f, 0.2f, dt);
        if (hoverAnim > 0.01f) fill(g, x, y, x + w, y + h, (int)(hoverAnim * 10) << 24 | 0x00FFFFFF);

        int ly = subtitle != null ? y + 5 : y + (h - 9) / 2;
        txts(g, label, x + 8, ly, dimmed ? C_TEXT_DIM : C_TEXT);
        drawSub(g, x + 8, ly);

        int tx = x + w - TW - 12, ty = y + (h - TH) / 2;

        // Track: dark when off, subtle green tint when on
        int trackColor = lerp(0xFF222240, 0xFF1A3A28, anim);
        if (dimmed) trackColor = 0xFF1E1E30;
        pill(g, tx, ty, TW, TH, trackColor);

        // Subtle border
        int brd = lerp(0xFF333350, 0xFF2A6A3A, anim);
        if (dimmed) brd = 0xFF2A2A3E;
        fill(g, tx + TH/2, ty, tx + TW - TH/2, ty + 1, brd);
        fill(g, tx + TH/2, ty + TH - 1, tx + TW - TH/2, ty + TH, brd);

        // Thumb
        int thumbX = tx + TR + (int)((TW - TR * 2) * anim);
        int thumbY = ty + TH / 2;

        // Glow when ON
        if (anim > 0.3f && !dimmed) {
            int gAlpha = (int)((anim - 0.3f) * 40);
            circle(g, thumbX, thumbY, TR + 3, (gAlpha << 24) | (C_ON & 0x00FFFFFF));
        }

        // Main thumb circle
        int thumbColor = dimmed ? 0xFF888899 : lerp(0xFFAABBCC, 0xFFFFFFFF, anim);
        circle(g, thumbX, thumbY, TR, thumbColor);

        // Green dot inside when ON
        if (anim > 0.5f && !dimmed) {
            int dotAlpha = (int)((anim - 0.5f) * 2 * 255);
            circle(g, thumbX, thumbY, 2, (dotAlpha << 24) | (C_ON & 0x00FFFFFF));
        }
    }

    @Override
    protected boolean onClick(double mx, double my, int btn) {
        if (btn != 0 || dimmed) return false;
        value = !value; onChange.accept(value); return true;
    }
}
