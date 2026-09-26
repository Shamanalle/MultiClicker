package pro.mikey.autoclicker.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Small drawing helpers for a flat, rounded look built from plain fills. */
public final class Draw {
    /** Per-column insets that approximate a rounded corner of radius 1..4. */
    private static final int[][] CORNERS = {{}, {1}, {2, 1}, {3, 1, 1}, {4, 2, 1, 1}};

    private Draw() {
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(Math.min(radius, 4), Math.min(w, h) / 2));
        g.fill(x + r, y, x + w - r, y + h, color);
        int[] insets = CORNERS[r];
        for (int i = 0; i < r; i++) {
            int inset = insets[i];
            g.fill(x + i, y + inset, x + i + 1, y + h - inset, color);
            g.fill(x + w - i - 1, y + inset, x + w - i, y + h - inset, color);
        }
    }

    /** A rounded rectangle with a 1px border. */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int radius, int fill, int border) {
        rect(g, x, y, w, h, radius, border);
        rect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void textRight(GuiGraphics g, Font font, String text, int right, int y, int color) {
        g.drawString(font, text, right - font.width(text), y, color, false);
    }

    public static void textCentered(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    /** Cuts the text with an ellipsis so that it fits into the given width. */
    public static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }
}
