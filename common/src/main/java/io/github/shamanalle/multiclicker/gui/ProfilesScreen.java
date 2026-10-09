package io.github.shamanalle.multiclicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Gfx;
import io.github.shamanalle.multiclicker.compat.Keys;
import io.github.shamanalle.multiclicker.config.ConfigManager;
import io.github.shamanalle.multiclicker.config.Preset;
import io.github.shamanalle.multiclicker.gui.widget.FlatButton;
import io.github.shamanalle.multiclicker.util.ServerStats;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Built-in presets, and saved profiles: save, load, delete, share as text, a hotkey for each
 * profile and the servers it loads on by itself.
 */
public class ProfilesScreen extends PanelScreen {
    private static final int ROW = 20;
    private static final long CONFIRM_MS = 3000;
    private static final int CHIP_HEIGHT = 14;

    private final MultiClicker mod = MultiClicker.get();
    private final KeyCapture capture = new KeyCapture();
    private final Map<String, Anim> chipHover = new HashMap<>();
    private List<String> profiles = List.of();
    private EditBox nameField;
    private FlatButton saveButton;
    /** Width of the header buttons (Copy and Back), so the loaded profile's name stays clear of them. */
    private int headerButtonsW;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private float scroll;
    private String pendingDelete;
    private long pendingDeleteSince;
    private Component status = Component.empty();
    private int statusColor = Theme.TEXT_MUTED;
    /** The server being played on, or {@code null} in the main menu. */
    @Nullable
    private String server;

    public ProfilesScreen(Screen parent) {
        super(parent, Component.translatable("multiclicker.gui.profiles"));
    }

    @Override
    protected int preferredWidth() {
        return 400;
    }

    @Override
    protected int preferredHeight() {
        return 320;
    }

    @Override
    protected void init() {
        super.init();
        profiles = mod.config().profiles();
        server = minecraft.player != null ? ServerStats.address(minecraft) : null;
        int inner = panelW - 20;

        Preset[] presets = Preset.values();
        int columns = 3;
        for (int i = 0; i < presets.length; i++) {
            Preset preset = presets[i];
            // The buttons of the last, shorter row share its whole width.
            int row = i / columns;
            int inRow = Math.min(columns, presets.length - row * columns);
            int presetW = (inner - 6 * (inRow - 1)) / inRow;
            int x = panelX + 10 + (i % columns) * (presetW + 6);
            int y = panelY + 48 + row * 22;
            button(preset.title(), preset == Preset.DEFAULTS ? FlatButton.Style.DANGER : FlatButton.Style.NORMAL,
                    () -> applyPreset(preset)).bounds(x, y, presetW, 18);
        }

        // Back and Copy (the current settings) sit in the header, so the list keeps its room on small screens.
        int backW = Math.max(46, font.width(I18n.get("gui.back")) + 16);
        int copyW = Math.max(46, font.width(I18n.get("multiclicker.gui.profiles.copy_current")) + 16);
        headerButtonsW = backW + copyW + 4;
        button(Component.translatable("gui.back"), FlatButton.Style.NORMAL, this::onClose)
                .bounds(panelX + panelW - 8 - backW, panelY + 7, backW, 16);
        button(Component.translatable("multiclicker.gui.profiles.copy_current"), FlatButton.Style.NORMAL, this::exportCurrent)
                .bounds(panelX + panelW - 8 - backW - 4 - copyW, panelY + 7, copyW, 16);

        int profilesTop = panelY + 48 + ((presets.length + columns - 1) / columns) * 22 + 18;
        int buttonW = Mth.clamp(Math.max(font.width(I18n.get("multiclicker.gui.profiles.overwrite")),
                font.width(I18n.get("multiclicker.gui.profiles.import"))) + 16, 56, 96);
        nameField = field(panelX + 10, profilesTop, inner - 2 * (buttonW + 6), Component.translatable("multiclicker.gui.profiles.name"));
        nameField.setMaxLength(32);
        nameField.setResponder(value -> updateSaveButton());
        saveButton = button(Component.translatable("multiclicker.gui.profiles.save"), FlatButton.Style.PRIMARY, this::saveProfile)
                .bounds(panelX + panelW - 10 - 2 * buttonW - 6, profilesTop, buttonW, 17);
        button(Component.translatable("multiclicker.gui.profiles.import"), FlatButton.Style.NORMAL, this::importProfile)
                .bounds(panelX + panelW - 10 - buttonW, profilesTop, buttonW, 17);
        updateSaveButton();

        listX = panelX + 10;
        listY = profilesTop + 22;
        listW = inner;
        listH = panelY + panelH - 22 - listY;
    }

    /** "Save" turns into "Overwrite" when a profile with that name exists. */
    private void updateSaveButton() {
        boolean exists = mod.config().hasProfile(nameField.getValue());
        saveButton.label = Component.translatable(exists ? "multiclicker.gui.profiles.overwrite" : "multiclicker.gui.profiles.save");
        saveButton.style = exists ? FlatButton.Style.DANGER : FlatButton.Style.PRIMARY;
    }

    private void refresh() {
        profiles = mod.config().profiles();
        updateSaveButton();
        mod.saveConfig();
    }

    private void applyPreset(Preset preset) {
        mod.restartModules(minecraft, () -> preset.apply(mod));
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
            refresh();
        } else {
            setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
        }
    }

    private void importProfile() {
        try {
            String name = mod.config().importProfile(minecraft.keyboardHandler.getClipboard(), nameField.getValue());
            setStatus(Component.translatable("multiclicker.gui.profiles.imported", name), Theme.SUCCESS);
            nameField.setValue("");
            refresh();
        } catch (IllegalArgumentException e) {
            setStatus(Component.translatable("multiclicker.gui.profiles.import_invalid"), Theme.WARNING);
        } catch (RuntimeException e) {
            MultiClicker.LOGGER.error("Failed to import a profile", e);
            setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
        }
    }

    private void exportCurrent() {
        String name = mod.config().activeProfile();
        minecraft.keyboardHandler.setClipboard(mod.config().exportCurrent(name != null ? name : ""));
        setStatus(Component.translatable("multiclicker.gui.profiles.copied_current"), Theme.SUCCESS);
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    // --- Rendering ------------------------------------------------------------------------------

    @Override
    protected void renderPanel(Canvas g, int mouseX, int mouseY, float delta) {
        String active = mod.config().activeProfile();
        if (active != null) {
            String label = I18n.get("multiclicker.gui.profiles.active", active);
            int titleEnd = panelX + 22 + font.width(title.copy().withStyle(ChatFormatting.BOLD));
            int right = panelX + panelW - 16 - headerButtonsW;
            Draw.textRight(g, font, Draw.ellipsize(font, label, right - titleEnd), right, panelY + 11, Theme.TEXT_MUTED);
        }
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
        Component tooltip = null;
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        if (profiles.isEmpty()) {
            Draw.textCentered(g, font, I18n.get("multiclicker.gui.profiles.empty"), listX + listW / 2,
                    listY + listH / 2 - 4, Theme.TEXT_MUTED);
        }
        int y = listY + 2 - Math.round(scroll);
        for (String name : profiles) {
            if (y + ROW > listY && y < listY + listH) {
                Component rowTooltip = drawProfile(g, name, y, mouseX, mouseY, delta);
                if (rowTooltip != null) {
                    tooltip = rowTooltip;
                }
            }
            y += ROW;
        }
        g.disableScissor();

        for (FlatButton button : buttons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                for (Preset preset : Preset.values()) {
                    if (button.label.equals(preset.title())) {
                        tooltip = preset.description();
                    }
                }
                if (button.label.getString().equals(I18n.get("multiclicker.gui.profiles.import"))) {
                    tooltip = Component.translatable("multiclicker.gui.profiles.import.desc");
                } else if (button.label.getString().equals(I18n.get("multiclicker.gui.profiles.copy_current"))) {
                    tooltip = Component.translatable("multiclicker.gui.profiles.copy_current.desc");
                }
            }
        }
        if (tooltip != null) {
            Gfx.tooltip(g, font, font.split(tooltip, 220), mouseX, mouseY);
        }
        Draw.textCentered(g, font, Draw.ellipsize(font, status.getString(), panelW - 20), panelX + panelW / 2,
                panelY + panelH - 15, statusColor);
    }

    /** The parts of a profile row, right to left: delete, load, copy, server, hotkey. */
    private record RowLayout(int deleteX, int loadX, int copyX, int serverX, int chipX, int chipW, String chipText) {
    }

    private RowLayout layout(String name) {
        String delete = name.equals(pendingDelete) ? I18n.get("multiclicker.gui.profiles.confirm_delete") : "✕";
        int deleteX = listX + listW - 8 - font.width(delete);
        int loadX = deleteX - 12 - font.width(I18n.get("multiclicker.gui.profiles.load"));
        int copyX = loadX - 18;
        int serverX = copyX - 16;
        String chipText = profileChipText(name);
        int chipW = Draw.chipWidth(font, chipText);
        int chipX = serverX - 6 - chipW;
        return new RowLayout(deleteX, loadX, copyX, serverX, chipX, chipW, chipText);
    }

    private String profileChipText(String name) {
        if (capture.isWaitingFor(name)) {
            return I18n.get("multiclicker.key.press");
        }
        String key = mod.config().meta(name).key();
        return key.isEmpty() ? null : Keys.parse(key).getDisplayName().getString();
    }

    @Nullable
    private Component drawProfile(Canvas g, String name, int y, int mouseX, int mouseY, float delta) {
        boolean overRow = isInList(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW;
        if (overRow) {
            g.fill(listX + 1, y, listX + listW - 1, y + ROW, Theme.ROW_HOVER);
        }
        RowLayout row = layout(name);
        ConfigManager.ProfileMeta meta = mod.config().meta(name);
        boolean active = name.equals(mod.config().activeProfile());
        int nameX = listX + 8;
        if (active) {
            Draw.rect(g, listX + 4, y + 7, 3, 6, 1, Theme.accent());
            nameX += 3;
        }
        Draw.text(g, font, Draw.ellipsize(font, name, row.chipX() - nameX - 6), nameX, y + 6, active ? Theme.accent() : Theme.TEXT);

        Component tooltip = null;
        boolean overChip = overRow && mouseX >= row.chipX() && mouseX < row.chipX() + row.chipW();
        float h = chipHover.computeIfAbsent(name, n -> new Anim(0)).update(overChip ? 1 : 0, delta, 20);
        boolean bound = !meta.key().isEmpty();
        if (bound || capture.isWaitingFor(name) || h > 0.01F) {
            Draw.chip(g, font, row.chipText(), row.chipX(), y + 3, CHIP_HEIGHT, h, capture.isWaitingFor(name), bound);
        } else {
            Draw.keyboardIcon(g, row.chipX() + (row.chipW() - 9) / 2, y + 7, Theme.alpha(Theme.TEXT_MUTED, 0.7F));
        }
        if (overChip) {
            tooltip = Component.translatable("multiclicker.gui.profiles.key.desc");
        }

        boolean onServer = server != null && meta.servers().contains(server);
        boolean overServer = overRow && mouseX >= row.serverX() - 3 && mouseX < row.copyX() - 3;
        int serverColor = server == null ? Theme.alpha(Theme.TEXT_MUTED, 0.4F)
                : onServer ? Theme.accent() : overServer ? Theme.TEXT : Theme.TEXT_MUTED;
        Draw.text(g, font, "⌂", row.serverX(), y + 6, serverColor);
        if (overServer) {
            tooltip = serverTooltip(meta, onServer);
        }

        boolean overCopy = overRow && mouseX >= row.copyX() - 3 && mouseX < row.loadX() - 6;
        Draw.text(g, font, "⧉", row.copyX(), y + 6, overCopy ? Theme.accent() : Theme.TEXT_MUTED);
        if (overCopy) {
            tooltip = Component.translatable("multiclicker.gui.profiles.copy.desc");
        }

        boolean overLoad = overRow && mouseX >= row.loadX() - 3 && mouseX < row.deleteX() - 6;
        boolean overDelete = overRow && mouseX >= row.deleteX() - 3;
        Draw.text(g, font, I18n.get("multiclicker.gui.profiles.load"), row.loadX(), y + 6, overLoad ? Theme.accent() : Theme.TEXT_DIM);
        String delete = name.equals(pendingDelete) ? I18n.get("multiclicker.gui.profiles.confirm_delete") : "✕";
        Draw.text(g, font, delete, row.deleteX(), y + 6, overDelete || name.equals(pendingDelete) ? Theme.DANGER : Theme.TEXT_MUTED);
        return tooltip;
    }

    private Component serverTooltip(ConfigManager.ProfileMeta meta, boolean onServer) {
        Component text;
        if (server == null) {
            text = Component.translatable("multiclicker.gui.profiles.server.no_world");
        } else {
            String where = server.equals(ConfigManager.SINGLEPLAYER) ? I18n.get("multiclicker.gui.profiles.singleplayer") : server;
            text = Component.translatable(onServer ? "multiclicker.gui.profiles.server.remove" : "multiclicker.gui.profiles.server.add", where);
        }
        if (!meta.servers().isEmpty()) {
            List<String> names = meta.servers().stream()
                    .map(s -> s.equals(ConfigManager.SINGLEPLAYER) ? I18n.get("multiclicker.gui.profiles.singleplayer") : s)
                    .toList();
            text = text.copy().append("\n").append(Component.translatable("multiclicker.gui.profiles.server.list", String.join(", ", names)));
        }
        return text;
    }

    private boolean isInList(double mouseX, double mouseY) {
        return mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH;
    }

    // --- Input ----------------------------------------------------------------------------------

    @Override
    protected boolean clicked(double mouseX, double mouseY, int button) {
        if (capture.isActive() && button >= 2) {
            capture.mouseClicked(button);
            return true;
        }
        capture.cancel();
        if ((button == 0 || button == 1) && isInList(mouseX, mouseY)) {
            int index = (int) ((mouseY - listY - 2 + scroll) / ROW);
            if (index >= 0 && index < profiles.size()) {
                handleProfileClick(profiles.get(index), mouseX, button);
                return true;
            }
        }
        return super.clicked(mouseX, mouseY, button);
    }

    private void handleProfileClick(String name, double mouseX, int button) {
        RowLayout row = layout(name);
        if (mouseX >= row.chipX() && mouseX < row.chipX() + row.chipW()) {
            setFocused(null);
            nameField.setFocused(false);
            if (button == 1) {
                mod.config().setProfileKey(name, "");
                refresh();
            } else {
                capture.start(name, key -> {
                    mod.config().setProfileKey(name, key.equals(InputConstants.UNKNOWN) ? "" : key.getName());
                    refresh();
                });
            }
            return;
        }
        if (button != 0) {
            return;
        }
        if (mouseX >= row.deleteX() - 3) {
            if (name.equals(pendingDelete)) {
                mod.config().deleteProfile(name);
                pendingDelete = null;
                setStatus(Component.translatable("multiclicker.gui.profiles.deleted", name), Theme.TEXT_MUTED);
                refresh();
            } else {
                pendingDelete = name;
                pendingDeleteSince = System.currentTimeMillis();
            }
        } else if (mouseX >= row.loadX() - 3) {
            if (mod.loadProfile(minecraft, name, "multiclicker.message.profile_loaded")) {
                setStatus(Component.translatable("multiclicker.gui.profiles.loaded", name), Theme.SUCCESS);
            } else {
                setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
            }
        } else if (mouseX >= row.copyX() - 3) {
            String text = mod.config().exportProfile(name);
            if (text != null) {
                minecraft.keyboardHandler.setClipboard(text);
                setStatus(Component.translatable("multiclicker.gui.profiles.copied", name), Theme.SUCCESS);
            } else {
                setStatus(Component.translatable("multiclicker.gui.profiles.error"), Theme.DANGER);
            }
        } else if (mouseX >= row.serverX() - 3) {
            if (server != null) {
                boolean added = mod.config().toggleServer(name, server);
                String where = server.equals(ConfigManager.SINGLEPLAYER) ? I18n.get("multiclicker.gui.profiles.singleplayer") : server;
                setStatus(Component.translatable(added ? "multiclicker.gui.profiles.server.added" : "multiclicker.gui.profiles.server.removed",
                        name, where), added ? Theme.SUCCESS : Theme.TEXT_MUTED);
                refresh();
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
    protected boolean pressed(int keyCode, int scanCode, int modifiers) {
        if (capture.keyPressed(keyCode)) {
            return true;
        }
        if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && nameField.isFocused()) {
            saveProfile();
            return true;
        }
        return super.pressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean typed(char codePoint, int modifiers) {
        return capture.swallowTyped() || super.typed(codePoint, modifiers);
    }
}
