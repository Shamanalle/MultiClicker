package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Screen base class with version independent input hooks. Screens override {@code clicked},
 * {@code dragged}, {@code released}, {@code scrolled}, {@code pressed} and {@code typed} instead of the vanilla
 * methods, whose signatures changed in Minecraft 1.21.9.
 */
public abstract class ModScreen extends Screen {
    private MouseButtonEvent activeEvent;
    private boolean activeDoubleClick;

    protected ModScreen(Component title) {
        super(title);
    }

    /**
     * Converts an SDL3 mouse button code (1 = left, 2 = middle, 3 = right, 4+ = side) used by
     * Minecraft 26.3+ into the GLFW convention (0 = left, 1 = right, 2 = middle, 3+ = side) used by
     * all MultiClicker screens and controls.
     */
    private static int toModButton(int sdlButton) {
        return switch (sdlButton) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
            default -> sdlButton - 1;
        };
    }

    private static int toSdlButton(int button) {
        return switch (button) {
            case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
            case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
            case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> button + 1;
        };
    }

    private MouseButtonEvent event(double mouseX, double mouseY, int button) {
        int modifiers = activeEvent != null ? activeEvent.modifiers() : 0;
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(toSdlButton(button), modifiers));
    }

    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g.graphics(), mouseX, mouseY, partialTick);
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Canvas(graphics), mouseX, mouseY, partialTick);
    }

    protected boolean clicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(event(mouseX, mouseY, button), activeDoubleClick);
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

    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        MouseButtonEvent prevEvent = activeEvent;
        boolean prevDoubleClick = activeDoubleClick;
        activeEvent = event;
        activeDoubleClick = doubleClick;
        try {
            return clicked(event.x(), event.y(), toModButton(event.button()));
        } finally {
            activeEvent = prevEvent;
            activeDoubleClick = prevDoubleClick;
        }
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent prevEvent = activeEvent;
        activeEvent = event;
        try {
            return dragged(event.x(), event.y(), toModButton(event.button()), dragX, dragY);
        } finally {
            activeEvent = prevEvent;
        }
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        MouseButtonEvent prevEvent = activeEvent;
        activeEvent = event;
        try {
            return released(event.x(), event.y(), toModButton(event.button()));
        } finally {
            activeEvent = prevEvent;
        }
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

    /** Dispatches a mouse click in MultiClicker's button convention (0 = left, 1 = right, 2 = middle). */
    public final boolean simulateClick(double mouseX, double mouseY, int button) {
        return clicked(mouseX, mouseY, button);
    }

    /** Dispatches a mouse release in MultiClicker's button convention (0 = left, 1 = right, 2 = middle). */
    public final boolean simulateRelease(double mouseX, double mouseY, int button) {
        return released(mouseX, mouseY, button);
    }
}
