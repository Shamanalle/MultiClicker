package io.github.shamanalle.multiclicker.gui;

import net.minecraft.client.gui.Font;
import io.github.shamanalle.multiclicker.compat.Canvas;

/** Small drawing helpers for a flat, rounded look built from plain fills. */
public final class Draw {
    /** Per-column insets that approximate a rounded corner of radius 1..4. */
    private static final int[][] CORNERS = {{}, {1}, {2, 1}, {3, 1, 1}, {4, 2, 1, 1}};

    private Draw() {
    }

    public static void rect(Canvas g, int x, int y, int w, int h, int radius, int color) {
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
    public static void box(Canvas g, int x, int y, int w, int h, int radius, int fill, int border) {
        rect(g, x, y, w, h, radius, border);
        rect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    public static void text(Canvas g, Font font, String text, int x, int y, int color) {
        g.text(font, text, x, y, color);
    }

    public static void textRight(Canvas g, Font font, String text, int right, int y, int color) {
        g.text(font, text, right - font.width(text), y, color);
    }

    public static void textCentered(Canvas g, Font font, String text, int centerX, int y, int color) {
        g.text(font, text, centerX - font.width(text) / 2, y, color);
    }

    /** Width of a key chip showing this text; {@code null} shows the keyboard icon instead. */
    public static int chipWidth(Font font, String text) {
        return text == null ? 18 : Math.max(18, font.width(text) + 10);
    }

    /** A tiny keyboard (9 x 6), for a hotkey that is not set yet. */
    public static void keyboardIcon(Canvas g, int x, int y, int color) {
        g.fill(x, y, x + 9, y + 1, color);
        g.fill(x, y + 5, x + 9, y + 6, color);
        g.fill(x, y + 1, x + 1, y + 5, color);
        g.fill(x + 8, y + 1, x + 9, y + 5, color);
        g.fill(x + 2, y + 2, x + 3, y + 3, color);
        g.fill(x + 4, y + 2, x + 5, y + 3, color);
        g.fill(x + 6, y + 2, x + 7, y + 3, color);
        g.fill(x + 3, y + 3, x + 6, y + 4, color);
    }

    /**
     * A small rounded "key cap" with a key name, as used for hotkeys.
     *
     * @param hover   0..1, how much the mouse is over it
     * @param waiting the chip waits for the player to press a key
     */
    public static void chip(Canvas g, Font font, String text, int x, int y, int h, float hover, boolean waiting, boolean bound) {
        int w = chipWidth(font, text);
        int accent = Theme.accent();
        int fill = waiting ? Theme.alpha(accent, 0.28F) : Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hover);
        int border = waiting ? accent : bound ? Theme.mix(Theme.CONTROL_HOVER, accent, 0.35F + 0.65F * hover)
                : Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hover);
        box(g, x, y, w, h, 3, fill, border);
        int color = waiting ? Theme.TEXT : bound ? Theme.TEXT : Theme.mix(Theme.TEXT_MUTED, Theme.TEXT_DIM, hover);
        if (text == null) {
            keyboardIcon(g, x + (w - 9) / 2, y + (h - 6) / 2, color);
        } else {
            textCentered(g, font, text, x + w / 2, y + (h - 8) / 2, color);
        }
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
