package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Screen base class with version independent input hooks. Screens override {@code clicked},
 * {@code dragged}, {@code released}, {@code scrolled}, {@code pressed} and {@code typed} instead of the vanilla
 * methods, whose signatures changed in Minecraft 1.20.2 and 1.21.9.
 */
public abstract class ModScreen extends Screen {
    protected ModScreen(Component title) {
        super(title);
    }

    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        super.render(g.pose(), mouseX, mouseY, partialTick);
    }

    @Override
    public final void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        draw(new Canvas(pose), mouseX, mouseY, partialTick);
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
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return scrolled(mouseX, mouseY, 0, scroll);
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
