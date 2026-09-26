package pro.mikey.autoclicker.module.visual;

/** Colors the user can pick for the interface accent and the target highlight. */
public enum Palette {
    BLUE(0xFF4F8CFF),
    VIOLET(0xFF9B6BFF),
    PINK(0xFFFF5FA2),
    RED(0xFFFF5C5C),
    ORANGE(0xFFFF9F43),
    YELLOW(0xFFF5C542),
    GREEN(0xFF3DDC84),
    TEAL(0xFF2DD4BF),
    WHITE(0xFFF2F4F8);

    private final int argb;

    Palette(int argb) {
        this.argb = argb;
    }

    public int argb() {
        return argb;
    }
}
