package io.github.shamanalle.multiclicker.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.module.session.AutoReconnectModule;

import java.util.List;

/** Shown instead of the vanilla "connection lost" screen while auto reconnect counts down. */
public class ReconnectScreen extends PanelScreen {
    private static final int MAX_REASON_LINES = 4;

    private final AutoReconnectModule module;
    private final Component reason;
    private final int attempt;
    private final int attempts;
    private final int totalTicks;
    private int ticksLeft;

    public ReconnectScreen(AutoReconnectModule module, Component reason, int attempt, int attempts, int ticks) {
        super(null, Component.translatable("multiclicker.reconnect.title"));
        this.module = module;
        this.reason = reason;
        this.attempt = attempt;
        this.attempts = attempts;
        this.totalTicks = Math.max(1, ticks);
        this.ticksLeft = ticks;
    }

    @Override
    protected int preferredWidth() {
        return 300;
    }

    @Override
    protected int preferredHeight() {
        return 96 + reasonLines().size() * 10;
    }

    private List<String> reasonLines() {
        List<String> lines = font.getSplitter().splitLines(reason.getString(), preferredWidth() - 20, Style.EMPTY)
                .stream().map(line -> line.getString()).toList();
        return lines.size() > MAX_REASON_LINES ? lines.subList(0, MAX_REASON_LINES) : lines;
    }

    @Override
    protected void init() {
        super.init();
        int inner = panelW - 20;
        int buttonW = (inner - 6) / 2;
        int y = panelY + panelH - 26;
        button(Component.translatable("multiclicker.reconnect.now"), FlatButton.Style.PRIMARY,
                () -> module.connect(minecraft)).bounds(panelX + 10, y, buttonW, 18);
        button(Component.translatable("gui.cancel"), FlatButton.Style.NORMAL, this::onClose)
                .bounds(panelX + panelW - 10 - buttonW, y, buttonW, 18);
    }

    @Override
    public void tick() {
        if (--ticksLeft <= 0) {
            module.connect(minecraft);
        }
    }

    @Override
    protected void renderPanel(Canvas g, int mouseX, int mouseY, float delta) {
        int y = panelY + 38;
        for (String line : reasonLines()) {
            Draw.textCentered(g, font, line, panelX + panelW / 2, y, Theme.TEXT_MUTED);
            y += 10;
        }
        y += 6;
        int seconds = (Math.max(0, ticksLeft) + 19) / 20;
        Draw.textCentered(g, font, Component.translatable("multiclicker.reconnect.countdown", seconds, attempt, attempts)
                .getString(), panelX + panelW / 2, y, Theme.TEXT);
        int barX = panelX + 20;
        int barW = panelW - 40;
        Draw.rect(g, barX, y + 14, barW, 4, 2, Theme.CONTROL);
        int filled = Math.round(barW * (1.0F - (float) ticksLeft / totalTicks));
        if (filled > 0) {
            Draw.rect(g, barX, y + 14, filled, 4, 2, Theme.accent());
        }
    }

    @Override
    public void onClose() {
        module.cancel(minecraft);
    }
}
