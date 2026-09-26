package pro.mikey.autoclicker.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.config.ConfigManager;
import pro.mikey.autoclicker.config.Preset;
import pro.mikey.autoclicker.gui.widget.FlatButton;

import java.util.List;
import java.util.Locale;

/** Built-in presets plus saving, loading and deleting named profiles. */
public class ProfilesScreen extends PanelScreen {
    private static final int ROW = 20;
    private static final long CONFIRM_MS = 3000;

    private final MultiClicker mod = MultiClicker.get();
    private List<String> profiles = List.of();
    private EditBox nameField;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private float scroll;
    private String pendingDelete;
    private long pendingDeleteSince;
    private Component status = Component.empty();
    private int statusColor = Theme.TEXT_MUTED;

    public ProfilesScreen(Screen parent) {
        super(parent, Component.translatable("multiclicker.gui.profiles"));
    }

    @Override
    protected int preferredWidth() {
        return 340;
    }

    @Override
    protected int preferredHeight() {
        return 300;
    }

    @Override
    protected void init() {
        super.init();
        profiles = mod.config().profiles();
        int inner = panelW - 20;

        Preset[] presets = Preset.values();
        int columns = 2;
        int presetW = (inner - 6) / columns;
        for (int i = 0; i < presets.length; i++) {
            Preset preset = presets[i];
            int x = panelX + 10 + (i % columns) * (presetW + 6);
            int y = panelY + 48 + (i / columns) * 22;
            button(preset.title(), preset == Preset.DEFAULTS ? FlatButton.Style.DANGER : FlatButton.Style.NORMAL,
                    () -> applyPreset(preset)).bounds(x, y, presetW, 18);
        }

        int profilesTop = panelY + 48 + ((presets.length + 1) / columns) * 22 + 18;
        int saveW = 70;
        nameField = field(panelX + 10, profilesTop, inner - saveW - 6, Component.translatable("multiclicker.gui.profiles.name"));
        nameField.setMaxLength(32);
        button(Component.translatable("multiclicker.gui.profiles.save"), FlatButton.Style.PRIMARY, this::saveProfile)
                .bounds(panelX + panelW - 10 - saveW, profilesTop, saveW, 17);

        listX = panelX + 10;
        listY = profilesTop + 22;
        listW = inner;
        listH = panelY + panelH - 34 - listY;
        button(Component.translatable("gui.back"), FlatButton.Style.NORMAL, this::onClose)
                .bounds(panelX + panelW / 2 - 50, panelY + panelH - 26, 100, 18);
    }

    private void applyPreset(Preset preset) {
        preset.apply(mod);
        mod.saveConfig();
        setStatus(Component.translatable("multiclicker.gui.profiles.preset_applied", preset.title()), Theme.SUCCESS);
    }

    private void saveProfile() {
        String name = ConfigManager.sanitizeName(nameField.getValue());
        if (name.isEmpty()) {
            setStatus(Component.translatable("multiclicker.gui.profiles.invalid_name"), Theme.WARNING);
            return;
        }
        if (mod.config().saveProfile(name)) {
            setStatus(Component.translatable("multiclicker.gui.profiles.saved", name), Theme.SUCCESS);
            nameField.setValue("");
            profiles = mod.config().profiles();
        } else {
            setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
        }
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    @Override
    protected void renderPanel(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Draw.text(g, font, I18n.get("multiclicker.gui.profiles.presets").toUpperCase(Locale.ROOT), panelX + 10, panelY + 37,
                Theme.alpha(Theme.accent(), 0.9F));
        Draw.text(g, font, I18n.get("multiclicker.gui.profiles.saved_profiles").toUpperCase(Locale.ROOT), panelX + 10,
                nameField.getY() - 15, Theme.alpha(Theme.accent(), 0.9F));
        drawFieldBackground(g, nameField);
        Draw.box(g, listX, listY, listW, listH, 3, Theme.CARD, Theme.CARD_BORDER);

        if (pendingDelete != null && System.currentTimeMillis() - pendingDeleteSince > CONFIRM_MS) {
            pendingDelete = null;
        }
        float maxScroll = Math.max(0, profiles.size() * ROW - (listH - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        if (profiles.isEmpty()) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.profiles.empty"), listX + listW / 2,
                    listY + listH / 2 - 4, Theme.TEXT_MUTED);
        }
        int y = listY + 2 - Math.round(scroll);
        for (String name : profiles) {
            if (y + ROW > listY && y < listY + listH) {
                drawProfile(g, name, y, mouseX, mouseY);
            }
            y += ROW;
        }
        g.disableScissor();

        for (FlatButton button : buttons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                for (Preset preset : Preset.values()) {
                    if (button.label.equals(preset.title())) {
                        g.setTooltipForNextFrame(font, font.split(preset.description(), 200), mouseX, mouseY);
                    }
                }
            }
        }
        Draw.textCentered(g, font, Draw.ellipsize(font, status.getString(), panelW - 20), panelX + panelW / 2,
                panelY + panelH - 36 + 4, statusColor);
    }

    private void drawProfile(GuiGraphics g, String name, int y, int mouseX, int mouseY) {
        boolean overRow = isInList(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW;
        if (overRow) {
            g.fill(listX + 1, y, listX + listW - 1, y + ROW, Theme.ROW_HOVER);
        }
        String load = I18n.get("multiclicker.gui.profiles.load");
        String delete = name.equals(pendingDelete) ? I18n.get("multiclicker.gui.profiles.confirm_delete") : "✕";
        int deleteX = listX + listW - 8 - font.width(delete);
        int loadX = deleteX - 12 - font.width(load);
        Draw.text(g, font, Draw.ellipsize(font, name, loadX - listX - 16), listX + 8, y + 6, Theme.TEXT);
        boolean overLoad = overRow && mouseX >= loadX - 3 && mouseX < deleteX - 6;
        boolean overDelete = overRow && mouseX >= deleteX - 3;
        Draw.text(g, font, load, loadX, y + 6, overLoad ? Theme.accent() : Theme.TEXT_DIM);
        Draw.text(g, font, delete, deleteX, y + 6,
                overDelete || name.equals(pendingDelete) ? Theme.DANGER : Theme.TEXT_MUTED);
    }

    private boolean isInList(double mouseX, double mouseY) {
        return mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInList(mouseX, mouseY)) {
            int index = (int) ((mouseY - listY - 2 + scroll) / ROW);
            if (index >= 0 && index < profiles.size()) {
                handleProfileClick(profiles.get(index), mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleProfileClick(String name, double mouseX) {
        String delete = name.equals(pendingDelete) ? I18n.get("multiclicker.gui.profiles.confirm_delete") : "✕";
        int deleteX = listX + listW - 8 - font.width(delete);
        int loadX = deleteX - 12 - font.width(I18n.get("multiclicker.gui.profiles.load"));
        if (mouseX >= deleteX - 3) {
            if (name.equals(pendingDelete)) {
                mod.config().deleteProfile(name);
                profiles = mod.config().profiles();
                pendingDelete = null;
                setStatus(Component.translatable("multiclicker.gui.profiles.deleted", name), Theme.TEXT_MUTED);
            } else {
                pendingDelete = name;
                pendingDeleteSince = System.currentTimeMillis();
            }
        } else if (mouseX >= loadX - 3) {
            if (mod.config().loadProfile(name)) {
                mod.saveConfig();
                setStatus(Component.translatable("multiclicker.gui.profiles.loaded", name), Theme.SUCCESS);
            } else {
                setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
            }
        } else {
            nameField.setValue(name);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInList(mouseX, mouseY)) {
            scroll -= (float) scrollY * ROW;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nameField.isFocused()) {
            saveProfile();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
