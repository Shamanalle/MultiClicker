package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.ListSetting;
import pro.mikey.autoclicker.setting.Setting;

/**
 * The interactive part of a setting row. Controls remember where they were drawn last frame and
 * use those bounds for mouse handling.
 */
public abstract class SettingControl {
    public static final int HEIGHT = 12;

    protected int x;
    protected int y;
    protected int width;
    protected int height = HEIGHT;

    public abstract int width(Font font);

    /** Draws the control with its left edge at {@code x}, vertically centered on the row. */
    public final void render(GuiGraphics g, Font font, int x, int rowY, int rowHeight, int mouseX, int mouseY, float delta) {
        this.width = width(font);
        this.x = x;
        this.y = rowY + (rowHeight - height) / 2;
        draw(g, font, mouseX, mouseY, delta);
    }

    protected abstract void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float delta);

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x - 2 && mouseX < x + width + 2 && mouseY >= y - 3 && mouseY < y + height + 3;
    }

    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);

    public boolean mouseDragged(double mouseX, double mouseY) {
        return false;
    }

    public void mouseReleased() {
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /** Arrow-key adjustment while the row is hovered. */
    public boolean adjust(int direction) {
        return false;
    }

    public static SettingControl of(Setting<?> setting, Screen screen) {
        if (setting instanceof BoolSetting bool) {
            return new ToggleControl(bool);
        }
        if (setting instanceof IntSetting integer) {
            return new SliderControl(integer);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            return new CycleControl(enumSetting);
        }
        if (setting instanceof ListSetting list) {
            return new ListControl(list, screen);
        }
        throw new IllegalArgumentException("Unsupported setting type: " + setting.getClass());
    }
}
