package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** GUI drawing calls that changed between Minecraft versions. */
public final class Gfx {
    private Gfx() {
    }

    /** Starts a transformed section: moved to (x, y) and scaled. End it with {@link #pop}. */
    public static void push(Canvas canvas, float x, float y, float scale) {
        canvas.graphics().pose().pushMatrix();
        canvas.graphics().pose().translate(x, y);
        canvas.graphics().pose().scale(scale, scale);
    }

    public static void pop(Canvas canvas) {
        canvas.graphics().pose().popMatrix();
    }

    public static void tooltip(Canvas canvas, Font font, List<FormattedCharSequence> lines, int x, int y) {
        canvas.graphics().setTooltipForNextFrame(font, lines, x, y);
    }
}
