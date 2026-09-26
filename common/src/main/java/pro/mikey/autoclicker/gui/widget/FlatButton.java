package pro.mikey.autoclicker.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import pro.mikey.autoclicker.gui.Anim;
import pro.mikey.autoclicker.gui.Draw;
import pro.mikey.autoclicker.gui.Theme;

/** A flat, rounded button in the MultiClicker style. */
public class FlatButton {
    public enum Style {
        NORMAL, PRIMARY, DANGER
    }

    private final Runnable action;
    private final Anim hover = new Anim(0);
    public Component label;
    public Style style;
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean active = true;

    public FlatButton(Component label, Style style, Runnable action) {
        this.label = label;
        this.style = style;
        this.action = action;
    }

    public FlatButton bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, float delta) {
        float hovered = hover.update(active && isMouseOver(mouseX, mouseY) ? 1 : 0, delta, 20);
        int base = switch (style) {
            case NORMAL -> Theme.CONTROL;
            case PRIMARY -> Theme.accent();
            case DANGER -> Theme.alpha(Theme.DANGER, 0.85F);
        };
        int hoverColor = style == Style.NORMAL ? Theme.CONTROL_HOVER : Theme.mix(base, 0xFFFFFFFF, 0.18F);
        int fill = active ? Theme.mix(base, hoverColor, hovered) : Theme.alpha(Theme.CONTROL, 0.5F);
        Draw.rect(g, x, y, width, height, 3, fill);
        int textColor = !active ? Theme.TEXT_MUTED : style == Style.NORMAL ? Theme.TEXT : 0xFFFFFFFF;
        String text = Draw.ellipsize(font, label.getString(), width - 8);
        Draw.textCentered(g, font, text, x + width / 2, y + (height - 8) / 2, textColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        action.run();
        return true;
    }
}
