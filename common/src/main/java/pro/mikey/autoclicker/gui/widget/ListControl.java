package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import pro.mikey.autoclicker.gui.Anim;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.ListEditScreen;
import pro.mikey.autoclicker.gui.Theme;
import pro.mikey.autoclicker.setting.ListSetting;

/** Shows the number of entries and opens the list editor. */
public class ListControl extends SettingControl {
    private final ListSetting setting;
    private final Screen parent;
    private final Anim hover = new Anim(0);

    public ListControl(ListSetting setting, Screen parent) {
        this.setting = setting;
        this.parent = parent;
        this.height = 14;
    }

    @Override
    public int width(Font font) {
        return Math.max(70, font.width(I18n.get("multiclicker.gui.edit_list", 999)) + 20);
    }

    @Override
    protected void draw(GuiGraphics g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(isMouseOver(mouseX, mouseY) ? 1 : 0, delta, 20);
        Draw.rect(g, x, y, width, height, 3, Theme.mix(Theme.CONTROL, Theme.CONTROL_HOVER, hovered));
        Draw.text(g, font, I18n.get("multiclicker.gui.edit_list", setting.get().size()), x + 6, y + 3, Theme.TEXT);
        Draw.textRight(g, font, "›", x + width - 5, y + 3, Theme.mix(Theme.TEXT_MUTED, Theme.accent(), hovered));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || button != 0) {
            return false;
        }
        Minecraft.getInstance().setScreen(new ListEditScreen(parent, setting));
        return true;
    }
}
