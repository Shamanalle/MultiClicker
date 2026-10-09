package io.github.shamanalle.multiclicker.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Keys;
import io.github.shamanalle.multiclicker.gui.Anim;
import io.github.shamanalle.multiclicker.gui.Draw;
import io.github.shamanalle.multiclicker.gui.Theme;
import io.github.shamanalle.multiclicker.setting.IntSetting;

/**
 * A slider with the formatted value next to it. Drag, scroll over the track or use arrow keys;
 * click the value to type an exact number.
 */
public class SliderControl extends SettingControl {
    private static final int MAX_TRACK = 80;
    private static final int MIN_TRACK = 32;
    private static final int GAP = 8;

    private final IntSetting setting;
    private final Anim fill;
    private final Anim hover = new Anim(0);
    private int valueWidth = -1;
    private int trackWidth = MAX_TRACK;
    private boolean dragging;
    private boolean editing;
    /** The first typed digit replaces the whole number, like a selected text field. */
    private boolean replaceOnType;
    private String editText = "";

    public SliderControl(IntSetting setting) {
        this.setting = setting;
        this.fill = new Anim(progress());
    }

    private float progress() {
        return (float) (setting.get() - setting.min()) / Math.max(1, setting.max() - setting.min());
    }

    @Override
    public void fit(Font font, int maxWidth) {
        width(font);
        trackWidth = Mth.clamp(maxWidth - GAP - valueWidth, MIN_TRACK, MAX_TRACK);
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
        return trackWidth + GAP + valueWidth;
    }

    private boolean overTrack(double mouseX, double mouseY) {
        return mouseX >= x - 2 && mouseX < x + trackWidth + 2 && mouseY >= y - 3 && mouseY < y + height + 3;
    }

    @Override
    protected void draw(Canvas g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(dragging || overTrack(mouseX, mouseY) ? 1 : 0, delta, 20);
        float shown = fill.update(progress(), delta, dragging ? 40 : 16);
        int trackY = y + height / 2 - 2;
        Draw.rect(g, x, trackY, trackWidth, 4, 2, Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered));
        int filled = Math.round(trackWidth * shown);
        Draw.rect(g, x, trackY, Math.max(filled, 2), 4, 2, Theme.accent());
        int knobX = Mth.clamp(x + filled - 3, x, x + trackWidth - 6);
        Draw.rect(g, knobX, y + 1, 6, height - 2, 2, 0xFFFFFFFF);
        if (editing) {
            drawEditor(g, font);
            return;
        }
        String text = Draw.ellipsize(font, setting.displayValue(), valueWidth);
        boolean overValue = isOverValue(mouseX, mouseY);
        int color = overValue ? Theme.accent() : setting.isDefault() ? Theme.TEXT_DIM : Theme.TEXT;
        Draw.textRight(g, font, text, x + width, y + 2, color);
    }

    private void drawEditor(Canvas g, Font font) {
        int boxX = x + trackWidth + GAP - 3;
        int boxW = width - trackWidth - GAP + 5;
        Draw.box(g, boxX, y - 2, boxW, height + 4, 2, Theme.CONTROL, Theme.accent());
        int textW = font.width(editText);
        int textX = x + width - textW - 4;
        if (replaceOnType && !editText.isEmpty()) {
            g.fill(textX - 1, y, textX + textW + 1, y + height, Theme.alpha(Theme.accent(), 0.45F));
        }
        Draw.text(g, font, editText, textX, y + 2, Theme.TEXT);
        if (System.currentTimeMillis() / 500 % 2 == 0) {
            g.fill(x + width - 3, y + 1, x + width - 2, y + height - 1, Theme.TEXT);
        }
    }

    /** Whether the point is over the number, which can be clicked to type a value. */
    public boolean isOverValue(double mouseX, double mouseY) {
        return mouseX >= x + trackWidth + GAP - 2 && mouseX < x + width + 2 && mouseY >= y - 3 && mouseY < y + height + 3;
    }

    public boolean isEditing() {
        return editing;
    }

    public void beginEdit() {
        editing = true;
        replaceOnType = true;
        editText = Integer.toString(setting.get());
    }

    /** Adds a typed digit; returns whether the character was used. */
    public boolean type(char character) {
        if (!editing || character < '0' || character > '9') {
            return false;
        }
        if (replaceOnType) {
            editText = "";
            replaceOnType = false;
        }
        if (editText.length() < 6) {
            editText += character;
        }
        return true;
    }

    public void backspace() {
        replaceOnType = false;
        if (!editText.isEmpty()) {
            editText = editText.substring(0, editText.length() - 1);
        }
    }

    /** Applies the typed number (clamped to the allowed range). */
    public void commit() {
        if (editing && !editText.isEmpty()) {
            setting.set(Integer.parseInt(editText));
        }
        editing = false;
    }

    public void cancelEdit() {
        editing = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            editing = false;
            setting.reset();
            return true;
        }
        if (overTrack(mouseX, mouseY)) {
            commit();
            dragging = true;
            setFromMouse(mouseX);
        } else if (button == 0 && isOverValue(mouseX, mouseY) && !editing) {
            beginEdit();
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
        int step = Keys.shiftDown() ? 10 : 1;
        setting.set(setting.get() + direction * step);
        return true;
    }

    private void setFromMouse(double mouseX) {
        double t = Mth.clamp((mouseX - x) / trackWidth, 0.0, 1.0);
        setting.set((int) Math.round(setting.min() + t * (setting.max() - setting.min())));
    }
}
