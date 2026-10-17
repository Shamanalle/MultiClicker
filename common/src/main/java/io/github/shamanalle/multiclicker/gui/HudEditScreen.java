package io.github.shamanalle.multiclicker.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Keys;
import io.github.shamanalle.multiclicker.compat.ModScreen;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.module.visual.HudModule;

import java.util.List;

/**
 * Places the status panel: drag it with the mouse (it snaps to the edges and the middle of the
 * screen), nudge it with the arrow keys and scale it with the mouse wheel.
 */
public class HudEditScreen extends ModScreen {
    private static final int SNAP = 6;

    @Nullable
    private final Screen parent;
    private final MultiClicker mod = MultiClicker.get();
    private final HudModule hud = mod.hud();
    private final FlatButton done = new FlatButton(Component.translatable("gui.done"), FlatButton.Style.PRIMARY,
            this::onClose);
    private final FlatButton reset = new FlatButton(Component.translatable("multiclicker.gui.hud_editor.reset"),
            FlatButton.Style.NORMAL, hud::placeInCorner);
    private final Anim outline = new Anim(0);

    private boolean dragging;
    private int grabX;
    private int grabY;
    /** The guides the panel snapped to while dragging, drawn as lines; -1 when none. */
    private int guideX = -1;
    private int guideY = -1;
    private long lastFrameNanos;

    public HudEditScreen(@Nullable Screen parent) {
        super(Component.translatable("multiclicker.gui.hud_editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        mod.saveConfig();
        Screens.open(minecraft, parent);
    }

    @Override
    protected void draw(Canvas g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0 ? 0 : Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
        lastFrameNanos = now;

        if (guideX >= 0) {
            g.fill(guideX, 0, guideX + 1, height, Theme.alpha(Theme.accent(), 0.6F));
        }
        if (guideY >= 0) {
            g.fill(0, guideY, width, guideY + 1, Theme.alpha(Theme.accent(), 0.6F));
        }
        hud.renderPreview(g);
        int[] b = hud.lastBounds();
        float h = outline.update(dragging || isOverHud(mouseX, mouseY) ? 1 : 0, delta, 16);
        int color = Theme.mix(Theme.alpha(0xFFFFFFFF, 0.35F), Theme.accent(), h);
        g.fill(b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] - 1, color);
        g.fill(b[0] - 2, b[1] + b[3] + 1, b[0] + b[2] + 2, b[1] + b[3] + 2, color);
        g.fill(b[0] - 2, b[1] - 1, b[0] - 1, b[1] + b[3] + 1, color);
        g.fill(b[0] + b[2] + 1, b[1] - 1, b[0] + b[2] + 2, b[1] + b[3] + 1, color);

        renderHelp(g, mouseX, mouseY, delta, b);
        super.draw(g, mouseX, mouseY, partialTick);
    }

    /** The hints and buttons, in the middle of the screen or, if the panel is there, below it. */
    private void renderHelp(Canvas g, int mouseX, int mouseY, float delta, int[] hudBounds) {
        List<String> lines = List.of(I18n.get("multiclicker.gui.hud_editor.drag"),
                I18n.get("multiclicker.gui.hud_editor.keys"),
                I18n.get("multiclicker.gui.hud_editor.scale", hud.scale.displayValue()));
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, font.width(line));
        }
        w = Math.min(width - 16, Math.max(180, w + 20));
        int h = 22 + lines.size() * 11 + 22;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        boolean overlaps = x < hudBounds[0] + hudBounds[2] && hudBounds[0] < x + w
                && y < hudBounds[1] + hudBounds[3] && hudBounds[1] < y + h;
        if (overlaps) {
            y = hudBounds[1] + hudBounds[3] / 2 < height / 2 ? height - h - 12 : 12;
        }
        Draw.box(g, x, y, w, h, 4, Theme.PANEL, Theme.CARD_BORDER);
        Draw.rect(g, x + 8, y + 8, 3, 10, 1, Theme.accent());
        Draw.text(g, font, title.getString(), x + 15, y + 9, Theme.TEXT);
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(g, font, Draw.ellipsize(font, lines.get(i), w - 20), x + 10, y + 24 + i * 11, Theme.TEXT_DIM);
        }
        int buttonY = y + h - 20;
        int buttonW = (w - 26) / 2;
        reset.bounds(x + 10, buttonY, buttonW, 14).render(g, font, mouseX, mouseY, delta);
        done.bounds(x + w - 10 - buttonW, buttonY, buttonW, 14).render(g, font, mouseX, mouseY, delta);
    }

    private boolean isOverHud(double mouseX, double mouseY) {
        int[] b = hud.lastBounds();
        return mouseX >= b[0] - 2 && mouseX < b[0] + b[2] + 2 && mouseY >= b[1] - 2 && mouseY < b[1] + b[3] + 2;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        if (done.mouseClicked(mouseX, mouseY, button) || reset.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && isOverHud(mouseX, mouseY)) {
            int[] b = hud.lastBounds();
            dragging = true;
            grabX = (int) mouseX - b[0];
            grabY = (int) mouseY - b[1];
            return true;
        }
        if (button == 1 && isOverHud(mouseX, mouseY)) {
            hud.placeInCorner();
            return true;
        }
        return super.clicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean dragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) {
            return super.dragged(mouseX, mouseY, button, dragX, dragY);
        }
        int[] b = hud.lastBounds();
        int x = (int) mouseX - grabX;
        int y = (int) mouseY - grabY;
        guideX = guideY = -1;
        // Shift places the panel freely.
        if (!Keys.shiftDown()) {
            int[] snappedX = snap(x, b[2], width);
            int[] snappedY = snap(y, b[3], height);
            x = snappedX[0];
            guideX = snappedX[1];
            y = snappedY[0];
            guideY = snappedY[1];
        }
        hud.moveTo(x, y, width, height, b[2], b[3]);
        return true;
    }

    /** Snaps a coordinate to the start, middle or end of the screen: the new value and the guide line, or -1. */
    private static int[] snap(int pos, int size, int screen) {
        int start = HudModule.MARGIN;
        int middle = (screen - size) / 2;
        int end = screen - size - HudModule.MARGIN;
        if (Math.abs(pos - start) <= SNAP) {
            return new int[] {start, -1};
        }
        if (Math.abs(pos - end) <= SNAP) {
            return new int[] {end, -1};
        }
        if (Math.abs(pos - middle) <= SNAP) {
            return new int[] {middle, screen / 2};
        }
        return new int[] {pos, -1};
    }

    @Override
    protected boolean released(double mouseX, double mouseY, int button) {
        dragging = false;
        guideX = guideY = -1;
        return super.released(mouseX, mouseY, button);
    }

    @Override
    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            // Keeps the panel's centre in place while it grows or shrinks.
            int[] before = hud.lastBounds();
            int oldScale = hud.scale.get();
            hud.scale.set(oldScale + (scrollY > 0 ? 10 : -10));
            if (!hud.isInCorner()) {
                float growth = hud.scale.get() / (float) oldScale;
                int newW = Math.round(before[2] * growth);
                int newH = Math.round(before[3] * growth);
                hud.moveTo(before[0] + (before[2] - newW) / 2, before[1] + (before[3] - newH) / 2, width, height, newW, newH);
            }
            return true;
        }
        return super.scrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        int step = Keys.shiftDown() ? 10 : 1;
        int dx = keyCode == InputConstants.KEY_LEFT ? -step : keyCode == InputConstants.KEY_RIGHT ? step : 0;
        int dy = keyCode == InputConstants.KEY_UP ? -step : keyCode == InputConstants.KEY_DOWN ? step : 0;
        if (dx != 0 || dy != 0) {
            int[] b = hud.lastBounds();
            hud.moveTo(b[0] + dx, b[1] + dy, width, height, b[2], b[3]);
            return true;
        }
        if (keyCode == InputConstants.KEY_R) {
            hud.placeInCorner();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            onClose();
            return true;
        }
        return super.pressed(keyCode, scanCode, modifiers);
    }
}
