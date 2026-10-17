package io.github.shamanalle.multiclicker.gui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.setting.StringSetting;

/** Editor for a text setting: a field, an optional test button, save and cancel. */
public class TextEditScreen extends PanelScreen {
    private final StringSetting setting;
    private EditBox input;
    private FlatButton save;
    @Nullable
    private FlatButton test;
    /** Written by the test request's thread, read while drawing. */
    private volatile Component status = Component.empty();
    private volatile int statusColor = Theme.TEXT_MUTED;
    private boolean testing;

    public TextEditScreen(Screen parent, StringSetting setting) {
        super(parent, setting.name());
        this.setting = setting;
    }

    @Override
    protected int preferredWidth() {
        return 340;
    }

    @Override
    protected int preferredHeight() {
        return setting.description() != null ? 136 : 112;
    }

    @Override
    protected void init() {
        String previous = input != null ? input.getValue() : setting.get();
        super.init();
        int inner = panelW - 20;
        int fieldY = panelY + panelH - 76;
        input = field(panelX + 10, fieldY, inner, Component.translatable("multiclicker.gui.text.hint"));
        input.setMaxLength(setting.maxLength());
        input.setValue(previous);
        input.setResponder(text -> validate());

        int buttonY = panelY + panelH - 26;
        int gap = 6;
        int count = setting.tester() != null ? 4 : 3;
        int buttonW = (inner - gap * (count - 1)) / count;
        int x = panelX + 10;
        if (setting.tester() != null) {
            test = button(Component.translatable("multiclicker.gui.text.test"), FlatButton.Style.NORMAL, this::runTest)
                    .bounds(x, buttonY, buttonW, 18);
            x += buttonW + gap;
        } else {
            test = null;
        }
        button(Component.translatable("multiclicker.gui.text.clear"), FlatButton.Style.DANGER, () -> input.setValue(""))
                .bounds(x, buttonY, buttonW, 18);
        x += buttonW + gap;
        button(Component.translatable("gui.cancel"), FlatButton.Style.NORMAL, this::onClose)
                .bounds(x, buttonY, buttonW, 18);
        x += buttonW + gap;
        save = button(Component.translatable("gui.done"), FlatButton.Style.PRIMARY, this::saveAndClose)
                .bounds(x, buttonY, panelX + panelW - 10 - x, 18);
        setInitialFocus(input);
        validate();
    }

    private void validate() {
        boolean valid = setting.isValid(input.getValue().strip());
        save.active = valid;
        if (test != null) {
            test.active = valid && !input.getValue().isBlank() && !testing;
        }
        if (!valid) {
            setStatus(Component.translatable("multiclicker.gui.text.invalid"), Theme.WARNING);
        } else if (statusColor == Theme.WARNING) {
            setStatus(Component.empty(), Theme.TEXT_MUTED);
        }
    }

    private void runTest() {
        var tester = setting.tester();
        if (tester == null || testing) {
            return;
        }
        testing = true;
        validate();
        setStatus(Component.translatable("multiclicker.gui.text.testing"), Theme.TEXT_MUTED);
        tester.apply(input.getValue().strip()).whenComplete((result, error) -> {
            testing = false;
            if (error != null || result == null) {
                setStatus(Component.translatable("multiclicker.gui.text.test_failed"), Theme.DANGER);
            } else {
                setStatus(result, Theme.TEXT);
            }
            minecraft.execute(this::validate);
        });
    }

    private void saveAndClose() {
        if (save.active) {
            setting.set(input.getValue());
            onClose();
        }
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    @Override
    protected void renderPanel(Canvas g, int mouseX, int mouseY, float delta) {
        Component description = setting.description();
        if (description != null) {
            int y = panelY + 38;
            for (var line : font.getSplitter().splitLines(description.getString(), panelW - 20, Style.EMPTY)) {
                if (y > panelY + panelH - 92) {
                    break;
                }
                Draw.text(g, font, line.getString(), panelX + 10, y, Theme.TEXT_DIM);
                y += 10;
            }
        }
        drawFieldBackground(g, input);
        Draw.text(g, font, Draw.ellipsize(font, status.getString(), panelW - 20), panelX + 10,
                panelY + panelH - 50, statusColor);
    }

    @Override
    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            saveAndClose();
            return true;
        }
        return super.pressed(keyCode, scanCode, modifiers);
    }
}
