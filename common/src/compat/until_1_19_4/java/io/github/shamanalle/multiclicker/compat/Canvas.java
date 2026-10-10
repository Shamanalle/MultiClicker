package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The 2D drawing surface of a screen or the HUD: before Minecraft 1.20 a pose stack and static helpers. */
public final class Canvas {
    private final PoseStack pose;

    public Canvas(PoseStack pose) {
        this.pose = pose;
    }

    /** The vanilla object, for the compat classes only. */
    PoseStack pose() {
        return pose;
    }

    public int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(pose, x1, y1, x2, y2, color);
    }

    public void text(Font font, String text, int x, int y, int color) {
        font.draw(pose, text, x, y, color);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        font.draw(pose, text, x, y, color);
    }

    public void item(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderGuiItem(pose, stack, x, y);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor((int) (x1 * scale), (int) (window.getHeight() - y2 * scale),
                (int) Math.max(0, (x2 - x1) * scale), (int) Math.max(0, (y2 - y1) * scale));
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }
}
