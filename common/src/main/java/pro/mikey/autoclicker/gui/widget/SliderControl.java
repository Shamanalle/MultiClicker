package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import pro.mikey.autoclicker.gui.Anim;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.Theme;
import pro.mikey.autoclicker.setting.IntSetting;

/** A slider with the formatted value next to it. Drag, scroll over the track or use arrow keys. */
public class SliderControl extends SettingControl {
    private static final int TRACK_WIDTH = 80;
    private static final int GAP = 8;

    private final IntSetting setting;
    private final Anim fill;
    private final Anim hover = new Anim(0);
    private int valueWidth = -1;
    private boolean dragging;

    public SliderControl(IntSetting setting) {
        this.setting = setting;
        this.fill = new Anim(progress());
    }

    private float progress() {
        return (float) (setting.get() - setting.min()) / Math.max(1, setting.max() - setting.min());
    }

    @Override
    public int width(Font font) {
        if (valueWidth < 0) {
            // Reserve space for the widest value so the track does not jump around while dragging.
            int widest = 0;
            int step = Math.max(1, (setting.max() - setting.min()) / 40);
            for (int value = setting.min(); value <= setting.max(); value += step) {
                widest = Math.max(widest, font.width(setting.format(value)));
            }
            widest = Math.max(widest, font.width(setting.format(setting.max())));
            valueWidth = Math.min(widest, 90);
        }
        return TRACK_WIDTH + GAP + valueWidth;
    }

    private boolean overTrack(double mouseX, double mouseY) {
        return mouseX >= x - 2 && mouseX < x + TRACK_WIDTH + 2 && mouseY >= y - 3 && mouseY < y + height + 3;
    }

    @Override
    protected void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(dragging || overTrack(mouseX, mouseY) ? 1 : 0, delta, 20);
        float shown = fill.update(progress(), delta, dragging ? 40 : 16);
        int trackY = y + height / 2 - 2;
        Draw.rect(g, x, trackY, TRACK_WIDTH, 4, 2, Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered));
        int filled = Math.round(TRACK_WIDTH * shown);
        Draw.rect(g, x, trackY, Math.max(filled, 2), 4, 2, Theme.accent());
        int knobX = Mth.clamp(x + filled - 3, x, x + TRACK_WIDTH - 6);
        Draw.rect(g, knobX, y + 1, 6, height - 2, 2, 0xFFFFFFFF);
        String text = Draw.ellipsize(font, setting.displayValue(), valueWidth);
        Draw.textRight(g, font, text, x + width, y + 2, setting.isDefault() ? Theme.TEXT_DIM : Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            setting.reset();
            return true;
        }
        if (overTrack(mouseX, mouseY)) {
            dragging = true;
            setFromMouse(mouseX);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (dragging) {
            setFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased() {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!overTrack(mouseX, mouseY) || amount == 0) {
            return false;
        }
        return adjust(amount > 0 ? 1 : -1);
    }

    @Override
    public boolean adjust(int direction) {
        int step = Screen.hasShiftDown() ? 10 : 1;
        setting.set(setting.get() + direction * step);
        return true;
    }

    private void setFromMouse(double mouseX) {
        double t = Mth.clamp((mouseX - x) / TRACK_WIDTH, 0.0, 1.0);
        setting.set((int) Math.round(setting.min() + t * (setting.max() - setting.min())));
    }
}
