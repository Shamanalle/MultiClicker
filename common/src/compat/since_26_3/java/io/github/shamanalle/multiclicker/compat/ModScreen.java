package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Screen base class with version independent input hooks. Screens override {@code clicked},
 * {@code dragged}, {@code released}, {@code pressed} and {@code typed} instead of the vanilla
 * methods, whose signatures changed in Minecraft 1.21.9.
 */
public abstract class ModScreen extends Screen {
    protected ModScreen(Component title) {
        super(title);
    }

    private static MouseButtonEvent event(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
    }

    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g.graphics(), mouseX, mouseY, partialTick);
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Canvas(graphics), mouseX, mouseY, partialTick);
    }

    protected boolean clicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(event(mouseX, mouseY, button), false);
    }

    protected boolean dragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(event(mouseX, mouseY, button), dragX, dragY);
    }

    protected boolean released(double mouseX, double mouseY, int button) {
        return super.mouseReleased(event(mouseX, mouseY, button));
    }

    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    protected boolean typed(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return clicked(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return dragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        return released(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        return pressed(event.key(), event.keycode(), event.modifiers());
    }

    @Override
    public final boolean charTyped(CharacterEvent event) {
        if (!Character.isBmpCodePoint(event.codepoint())) {
            return super.charTyped(event);
        }
        return typed((char) event.codepoint(), 0);
    }
}
