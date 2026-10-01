package io.github.shamanalle.multiclicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.ModScreen;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;

import java.util.ArrayList;
import java.util.List;

/** Base for the smaller MultiClicker dialogs: a centered panel with a title and flat buttons. */
public abstract class PanelScreen extends ModScreen {
    protected final Screen parent;
    protected final List<FlatButton> buttons = new ArrayList<>();
    protected int panelX;
    protected int panelY;
    protected int panelW;
    protected int panelH;
    private long lastFrameNanos;

    protected PanelScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    protected abstract int preferredWidth();

    protected abstract int preferredHeight();

    @Override
    protected void init() {
        buttons.clear();
        panelW = Math.min(width - 24, preferredWidth());
        panelH = Math.min(height - 24, preferredHeight());
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    protected FlatButton button(Component label, FlatButton.Style style, Runnable action) {
        FlatButton button = new FlatButton(label, style, action);
        buttons.add(button);
        return button;
    }

    /** A borderless text field; draw its background with {@link #drawFieldBackground}. */
    protected EditBox field(int x, int y, int width, Component hint) {
        EditBox box = new EditBox(font, x + 6, y + 4, width - 12, 10, hint);
        box.setBordered(false);
        box.setTextColor(Theme.TEXT);
        box.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));
        return addRenderableWidget(box);
    }

    protected void drawFieldBackground(Canvas g, EditBox box) {
        Draw.box(g, box.getX() - 6, box.getY() - 4, box.getWidth() + 12, 17, 3, Theme.CONTROL,
                box.isFocused() ? Theme.alpha(Theme.accent(), 0.8F) : Theme.CONTROL);
    }

    @Override
    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0 ? 0 : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = now;

        Draw.box(g, panelX, panelY, panelW, panelH, 4, Theme.PANEL, Theme.CARD_BORDER);
        Draw.rect(g, panelX + 8, panelY + 10, 3, 10, 1, Theme.accent());
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), panelX + 15, panelY + 11, Theme.TEXT);
        g.fill(panelX + 1, panelY + 29, panelX + panelW - 1, panelY + 30, Theme.DIVIDER);

        renderPanel(g, mouseX, mouseY, delta);
        for (FlatButton button : buttons) {
            button.render(g, font, mouseX, mouseY, delta);
        }
        super.draw(g, mouseX, mouseY, partialTick);
    }

    protected abstract void renderPanel(Canvas g, int mouseX, int mouseY, float delta);

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        for (FlatButton flatButton : List.copyOf(buttons)) {
            if (flatButton.mouseClicked(mouseX, mouseY, button)) {
                setFocused(null);
                return true;
            }
        }
        boolean handled = super.clicked(mouseX, mouseY, button);
        if (!handled) {
            setFocused(null);
        }
        return handled;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Screens.open(minecraft, parent);
    }
}
