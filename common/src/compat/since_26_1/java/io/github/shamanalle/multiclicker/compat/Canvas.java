package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The 2D drawing surface of a screen or the HUD (the vanilla class was renamed in newer versions). */
public final class Canvas {
    private final GuiGraphicsExtractor graphics;

    public Canvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    /** The vanilla object, for the compat classes only. */
    GuiGraphicsExtractor graphics() {
        return graphics;
    }

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public void text(Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        graphics.disableScissor();
    }
}
