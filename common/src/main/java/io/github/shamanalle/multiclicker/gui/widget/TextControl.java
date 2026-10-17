package io.github.shamanalle.multiclicker.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.gui.Anim;
import io.github.shamanalle.multiclicker.gui.Draw;
import io.github.shamanalle.multiclicker.gui.TextEditScreen;
import io.github.shamanalle.multiclicker.gui.Theme;
import io.github.shamanalle.multiclicker.setting.StringSetting;

/** Shows the start of the text and opens the editor; right-click clears it. */
public class TextControl extends SettingControl {
    private static final int WIDTH = 110;

    private final StringSetting setting;
    private final Screen parent;
    private final Anim hover = new Anim(0);

    public TextControl(StringSetting setting, Screen parent) {
        this.setting = setting;
        this.parent = parent;
        this.height = 14;
    }

    @Override
    public int width(Font font) {
        return WIDTH;
    }

    @Override
    protected void draw(Canvas g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(isMouseOver(mouseX, mouseY) ? 1 : 0, delta, 20);
        Draw.rect(g, x, y, width, height, 3, Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered));
        int color = setting.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT;
        Draw.text(g, font, Draw.ellipsize(font, setting.displayValue(), width - 20), x + 6, y + 3, color);
        Draw.textRight(g, font, "✎", x + width - 5, y + 3, Theme.mix(Theme.TEXT_MUTED, Theme.accent(), hovered));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            setting.reset();
        } else if (button == 0) {
            Screens.open(Minecraft.getInstance(), new TextEditScreen(parent, setting));
        }
        return true;
    }
}
