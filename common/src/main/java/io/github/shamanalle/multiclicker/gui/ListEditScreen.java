package io.github.shamanalle.multiclicker.gui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Ids;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.setting.ListSetting;

import java.util.List;

/** Editor for item / block id lists with icons, validation and quick-add from the game. */
public class ListEditScreen extends PanelScreen {
    private static final int ROW = 20;

    private final ListSetting setting;
    private EditBox input;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private float scroll;
    private Component status = Component.empty();
    private int statusColor = Theme.TEXT_MUTED;

    public ListEditScreen(Screen parent, ListSetting setting) {
        super(parent, setting.name());
        this.setting = setting;
    }

    @Override
    protected int preferredWidth() {
        return 320;
    }

    @Override
    protected int preferredHeight() {
        return 280;
    }

    @Override
    protected void init() {
        super.init();
        int inner = panelW - 20;
        int addW = Math.max(54, font.width(Component.translatable("multiclicker.gui.list.add")) + 16);
        input = field(panelX + 10, panelY + 38, inner - addW - 6, Component.translatable("multiclicker.gui.list.hint"));
        input.setMaxLength(100);
        button(Component.translatable("multiclicker.gui.list.add"), FlatButton.Style.PRIMARY, this::addFromInput)
                .bounds(panelX + panelW - 10 - addW, panelY + 38, addW, 17);

        boolean items = setting.kind() == ListSetting.Kind.ITEM;
        button(Component.translatable(items ? "multiclicker.gui.list.add_held" : "multiclicker.gui.list.add_looked"),
                FlatButton.Style.NORMAL, items ? this::addHeldItem : this::addLookedAtBlock)
                .bounds(panelX + 10, panelY + 61, inner, 16);

        listX = panelX + 10;
        listY = panelY + 84;
        listW = inner;
        listH = panelH - 84 - 36;
        button(Component.translatable("gui.done"), FlatButton.Style.PRIMARY, this::onClose)
                .bounds(panelX + panelW / 2 - 50, panelY + panelH - 26, 100, 18);
        setInitialFocus(input);
    }

    private void addFromInput() {
        String raw = input.getValue();
        if (raw.isBlank()) {
            return;
        }
        String id = ListSetting.normalize(raw);
        if (!setting.add(raw)) {
            setStatus(Component.translatable("multiclicker.gui.list.duplicate", id), Theme.WARNING);
        } else if (!setting.isKnown(id)) {
            setStatus(Component.translatable("multiclicker.gui.list.unknown", id), Theme.WARNING);
        } else {
            setStatus(Component.translatable("multiclicker.gui.list.added", id), Theme.SUCCESS);
        }
        input.setValue("");
    }

    private void addHeldItem() {
        ItemStack stack = minecraft.player != null ? minecraft.player.getMainHandItem() : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            setStatus(Component.translatable("multiclicker.gui.list.no_item"), Theme.WARNING);
            return;
        }
        addId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private void addLookedAtBlock() {
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            setStatus(Component.translatable("multiclicker.gui.list.no_block"), Theme.WARNING);
            return;
        }
        addId(BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(hit.getBlockPos()).getBlock()).toString());
    }

    private void addId(String id) {
        if (setting.add(id)) {
            setStatus(Component.translatable("multiclicker.gui.list.added", id), Theme.SUCCESS);
        } else {
            setStatus(Component.translatable("multiclicker.gui.list.duplicate", id), Theme.WARNING);
        }
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    @Override
    protected void renderPanel(Canvas g, int mouseX, int mouseY, float delta) {
        drawFieldBackground(g, input);
        Draw.box(g, listX, listY, listW, listH, 3, Theme.CARD, Theme.CARD_BORDER);

        List<String> entries = setting.get();
        float maxScroll = Math.max(0, entries.size() * ROW - (listH - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        if (entries.isEmpty()) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.list.empty"), listX + listW / 2, listY + listH / 2 - 4,
                    Theme.TEXT_MUTED);
        }
        int y = listY + 2 - Math.round(scroll);
        for (String id : entries) {
            if (y + ROW > listY && y < listY + listH) {
                drawEntry(g, id, y, mouseX, mouseY);
            }
            y += ROW;
        }
        g.disableScissor();

        Draw.textCentered(g, font, Draw.ellipsize(font, status.getString(), panelW - 20), panelX + panelW / 2,
                panelY + panelH - 36 + 3, statusColor);
    }

    private void drawEntry(Canvas g, String id, int y, int mouseX, int mouseY) {
        boolean known = setting.isKnown(id);
        boolean overRow = isInList(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW;
        if (overRow) {
            g.fill(listX + 1, y, listX + listW - 1, y + ROW, Theme.ROW_HOVER);
        }
        ItemStack icon = iconFor(id);
        if (!icon.isEmpty()) {
            g.item(icon, listX + 5, y + 2);
        }
        String text = Draw.ellipsize(font, known ? id : id + "  (?)", listW - 50);
        Draw.text(g, font, text, listX + 26, y + 6, known ? Theme.TEXT : Theme.DANGER);
        boolean overRemove = overRow && mouseX >= listX + listW - 20;
        Draw.textCentered(g, font, "✕", listX + listW - 11, y + 6, overRemove ? Theme.DANGER : Theme.TEXT_MUTED);
    }

    private ItemStack iconFor(String id) {
        if (!setting.isKnown(id)) {
            return ItemStack.EMPTY;
        }
        try {
            return setting.kind() == ListSetting.Kind.ITEM
                    ? new ItemStack(Ids.item(id))
                    : new ItemStack(Ids.block(id).asItem());
        } catch (RuntimeException e) {
            // Newer versions bind item data only after a world was loaded: no icon until then.
            return ItemStack.EMPTY;
        }
    }

    private boolean isInList(double mouseX, double mouseY) {
        return mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInList(mouseX, mouseY) && mouseX >= listX + listW - 20) {
            int index = (int) ((mouseY - listY - 2 + scroll) / ROW);
            List<String> entries = setting.get();
            if (index >= 0 && index < entries.size()) {
                String id = entries.get(index);
                setting.remove(id);
                setStatus(Component.translatable("multiclicker.gui.list.removed", id), Theme.TEXT_MUTED);
                return true;
            }
        }
        return super.clicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInList(mouseX, mouseY)) {
            scroll -= (float) scrollY * ROW;
            return true;
        }
        return super.scrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && input.isFocused()) {
            addFromInput();
            return true;
        }
        return super.pressed(keyCode, scanCode, modifiers);
    }
}
