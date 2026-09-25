package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import pro.mikey.autoclicker.gui.Anim;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.Theme;
import pro.mikey.autoclicker.setting.BoolSetting;

/** An animated on/off switch. */
public class ToggleControl extends SettingControl {
    private final BoolSetting setting;
    private final Anim knob;
    private final Anim hover = new Anim(0);

    public ToggleControl(BoolSetting setting) {
        this.setting = setting;
        this.knob = new Anim(setting.get() ? 1 : 0);
        this.height = 12;
    }

    @Override
    public int width(Font font) {
        return 22;
    }

    @Override
    protected void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float delta) {
        float on = knob.update(setting.get() ? 1 : 0, delta, 18);
        float hovered = hover.update(isMouseOver(mouseX, mouseY) ? 1 : 0, delta, 20);
        int off = Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered);
        int track = Theme.mix(off, Theme.accent(), on);
        Draw.rect(g, x, y, width, height, 4, track);
        int knobSize = height - 4;
        int knobX = Math.round(x + 2 + (width - knobSize - 4) * on);
        Draw.rect(g, knobX, y + 2, knobSize, knobSize, 3, Theme.mix(0xFFB8BFCC, 0xFFFFFFFF, on));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            setting.reset();
        } else {
            setting.toggle();
        }
        return true;
    }

    @Override
    public boolean adjust(int direction) {
        setting.set(direction > 0);
        return true;
    }
}
