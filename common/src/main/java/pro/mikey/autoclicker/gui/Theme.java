package pro.mikey.autoclicker.gui;

import pro.mikey.autoclicker.MultiClicker;

/** Colors of the MultiClicker interface (ARGB). */
public final class Theme {
    public static final int PANEL = 0xF2121419;
    public static final int SIDEBAR = 0xFF0E1014;
    public static final int CARD = 0xFF171A21;
    public static final int CARD_BORDER = 0xFF232833;
    public static final int ROW_HOVER = 0x0FFFFFFF;
    public static final int DIVIDER = 0xFF20242D;
    public static final int CONTROL = 0xFF262B36;
    public static final int CONTROL_HOVER = 0xFF2F3542;
    public static final int TEXT = 0xFFE9ECF1;
    public static final int TEXT_DIM = 0xFFA3ABBA;
    public static final int TEXT_MUTED = 0xFF687084;
    public static final int SUCCESS = 0xFF3DDC84;
    public static final int DANGER = 0xFFFF5C5C;
    public static final int WARNING = 0xFFF5C542;
    public static final int HUD_BACKGROUND = 0xB80E1014;

    private Theme() {
    }

    public static int accent() {
        MultiClicker mod = MultiClicker.get();
        return mod != null ? mod.ui().accentColor() : 0xFF4F8CFF;
    }

    /** Replaces the alpha channel (0..1) of a color. */
    public static int alpha(int color, float alpha) {
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** Linear blend between two ARGB colors. */
    public static int mix(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = lerp(from >>> 24, to >>> 24, t);
        int r = lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
