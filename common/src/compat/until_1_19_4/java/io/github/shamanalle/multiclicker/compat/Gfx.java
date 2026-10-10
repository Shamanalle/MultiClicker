package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** GUI drawing calls that changed between Minecraft versions. */
public final class Gfx {
    private Gfx() {
    }

    /** Starts a transformed section: moved to (x, y) and scaled. End it with {@link #pop}. */
    public static void push(Canvas canvas, float x, float y, float scale) {
        canvas.pose().pushPose();
        canvas.pose().translate(x, y, 0);
        canvas.pose().scale(scale, scale, 1);
    }

    public static void pop(Canvas canvas) {
        canvas.pose().popPose();
    }

    public static void tooltip(Canvas canvas, Font font, List<FormattedCharSequence> lines, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            mc.screen.renderTooltip(canvas.pose(), lines, x, y);
        }
    }
}
