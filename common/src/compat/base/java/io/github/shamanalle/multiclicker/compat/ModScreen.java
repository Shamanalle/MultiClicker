package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Screen base class with version independent input hooks. Screens override {@code clicked},
 * {@code dragged}, {@code released}, {@code scrolled}, {@code pressed} and {@code typed} instead of the vanilla
 * methods, whose signatures changed in Minecraft 1.21.9.
 */
public abstract class ModScreen extends Screen {
    protected ModScreen(Component title) {
        super(title);
    }

    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        super.render(g.graphics(), mouseX, mouseY, partialTick);
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Canvas(graphics), mouseX, mouseY, partialTick);
    }

    protected boolean clicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean dragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected boolean released(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean typed(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return clicked(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return dragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return released(mouseX, mouseY, button);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return pressed(keyCode, scanCode, modifiers);
    }

    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return typed(codePoint, modifiers);
    }
}
