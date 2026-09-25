package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import pro.mikey.autoclicker.gui.Anim;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.Theme;
import pro.mikey.autoclicker.setting.EnumSetting;

/** "‹ Value ›" selector: the left half steps back, the right half steps forward. */
public class CycleControl extends SettingControl {
    private final EnumSetting<?> setting;
    private final Anim hover = new Anim(0);
    private int cachedWidth = -1;

    public CycleControl(EnumSetting<?> setting) {
        this.setting = setting;
        this.height = 14;
    }

    @Override
    public int width(Font font) {
        if (cachedWidth < 0) {
            int widest = 0;
            for (Enum<?> value : setting.values()) {
                widest = Math.max(widest, font.width(EnumSetting.label(value)));
            }
            cachedWidth = Math.max(70, widest + 28);
        }
        return cachedWidth;
    }

    @Override
    protected void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float delta) {
        boolean over = isMouseOver(mouseX, mouseY);
        float hovered = hover.update(over ? 1 : 0, delta, 20);
        Draw.rect(g, x, y, width, height, 3, Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered));
        boolean leftHalf = mouseX < x + width / 2;
        int arrowIdle = Theme.TEXT_MUTED;
        Draw.text(g, font, "‹", x + 5, y + 3, over && leftHalf ? Theme.accent() : arrowIdle);
        Draw.textRight(g, font, "›", x + width - 5, y + 3, over && !leftHalf ? Theme.accent() : arrowIdle);
        String label = Draw.ellipsize(font, setting.displayValue(), width - 24);
        Draw.textCentered(g, font, label, x + width / 2, y + 3, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            setting.reset();
        } else {
            setting.cycle(mouseX < x + width / 2.0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean adjust(int direction) {
        setting.cycle(direction);
        return true;
    }
}
