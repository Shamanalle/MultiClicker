package io.github.shamanalle.multiclicker.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.gui.Anim;
import io.github.shamanalle.multiclicker.gui.Draw;
import io.github.shamanalle.multiclicker.gui.KeyCapture;
import io.github.shamanalle.multiclicker.setting.KeySetting;
import io.github.shamanalle.multiclicker.util.KeyConflicts;

/** A hotkey: click and press a key to bind it, right-click to remove it. */
public class KeyControl extends SettingControl {
    private final KeySetting setting;
    private final KeyCapture capture;
    private final Anim hover = new Anim(0);

    public KeyControl(KeySetting setting, KeyCapture capture) {
        this.setting = setting;
        this.capture = capture;
        this.height = 14;
    }

    private String label() {
        return capture.isWaitingFor(setting) ? I18n.get("multiclicker.key.press") : setting.displayValue();
    }

    @Override
    public int width(Font font) {
        return Math.max(Draw.chipWidth(font, label()), Draw.chipWidth(font, I18n.get("multiclicker.key.press")));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        if (font == null) {
            return super.isMouseOver(mouseX, mouseY);
        }
        int chipW = Draw.chipWidth(font, label());
        int chipX = x + width - chipW;
        return mouseX >= chipX - 2 && mouseX < x + width + 2 && mouseY >= y - 3 && mouseY < y + height + 3;
    }

    @Override
    protected void draw(Canvas g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(isMouseOver(mouseX, mouseY) ? 1 : 0, delta, 20);
        String text = label();
        int chipW = Draw.chipWidth(font, text);
        Draw.chip(g, font, text, x + width - chipW, y, height, hovered, capture.isWaitingFor(setting), setting.isBound(),
                !KeyConflicts.others(setting).isEmpty());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || (button != 0 && button != 1)) {
            return false;
        }
        if (button == 1) {
            capture.cancel();
            setting.clear();
        } else {
            capture.start(setting, setting::bind);
        }
        return true;
    }
}
