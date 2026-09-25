package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphics;
import pro.mikey.autoclicker.core.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * McStringListEditor — editable tag-chip list widget for StringListSetting.
 *
 * Layout:
 *   Row 0: Label + subtitle (description)
 *   Row 1: [text input field ___________] [+]
 *   Rows 2+: Flowing tag chips: [diamond_ore ×] [stone ×] ...
 *
 * Features:
 *   - Type block ID → Enter or click [+] to add
 *   - Click [×] on a chip to remove
 *   - Backspace to delete characters in input
 *   - Dynamic height based on chip count
 */
public class McStringListEditor extends McWidget {

    private final Setting.StringListSetting setting;
    private String inputText = "";
    private boolean inputFocused;
    private int blinkTick;

    // Layout constants
    private static final int LABEL_H = 22;      // label + subtitle zone
    private static final int INPUT_H = 16;       // text input row height
    private static final int INPUT_PAD = 4;      // padding around input row
    private static final int CHIP_H = 14;        // chip height
    private static final int CHIP_PAD_X = 4;     // horizontal padding inside chip
    private static final int CHIP_GAP = 4;       // gap between chips
    private static final int CHIP_ROW_GAP = 3;   // gap between chip rows
    private static final int ADD_BTN_W = 18;     // [+] button width
    private static final int SECTION_GAP = 4;    // gap between input and chips area
    private static final int BOTTOM_PAD = 4;     // bottom padding

    public McStringListEditor(int x, int y, int w, Setting.StringListSetting setting) {
        super(x, y, w, 0, setting.getDisplayName());
        this.setting = setting;
        recalcHeight();
    }

    // ═══════════════════════════════════════════════════════════════
    // Height calculation — dynamic based on number of chip rows
    // ═══════════════════════════════════════════════════════════════

    private void recalcHeight() {
        h = LABEL_H + INPUT_PAD + INPUT_H + INPUT_PAD;
        List<String> items = setting.get();
        if (!items.isEmpty()) {
            h += SECTION_GAP;
            int chipRows = countChipRows();
            h += chipRows * (CHIP_H + CHIP_ROW_GAP);
        }
        h += BOTTOM_PAD;
    }

    private int countChipRows() {
        List<String> items = setting.get();
        if (items.isEmpty()) return 0;

        int contentW = w - 16; // left/right margin
        int cx = 0;
        int rows = 1;
        for (String item : items) {
            int chipW = chipWidth(item);
            if (cx > 0 && cx + chipW > contentW) {
                rows++;
                cx = 0;
            }
            cx += chipW + CHIP_GAP;
        }
        return rows;
    }

    private int chipWidth(String text) {
        return tw(text) + CHIP_PAD_X * 2 + tw("×") + 6 + 4; // text + padding + x-btn + gaps
    }

    @Override
    public int scrollHeight() {
        recalcHeight();
        return h;
    }

    // ═══════════════════════════════════════════════════════════════
    // Rendering
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void draw(GuiGraphics g, int mx, int my, float dt) {
        blinkTick++;
        recalcHeight();

        // Hover highlight
        if (hoverAnim > 0.01f) {
            fill(g, x, y, x + w, y + h, (int)(hoverAnim * 8) << 24 | 0x00FFFFFF);
        }

        int cy = y;

        // ── Label + subtitle ──
        int ly = subtitle != null ? cy + 4 : cy + (LABEL_H - 9) / 2;
        txts(g, label, x + 8, ly, dimmed ? C_TEXT_DIM : C_TEXT);
        if (subtitle != null) {
            drawSub(g, x + 8, ly);
        }
        cy += LABEL_H;

        // ── Input field + [+] button ──
        cy += INPUT_PAD;
        int inputX = x + 8;
        int inputW = w - 16 - ADD_BTN_W - 4;
        int inputY = cy;

        // Input background
        int inputBg = inputFocused ? 0xC0181830 : 0x40181830;
        int inputBorder = inputFocused ? accent() : 0x22FFFFFF;
        panel(g, inputX, inputY, inputW, INPUT_H, inputBg, inputBorder);

        // Input text / placeholder
        if (inputText.isEmpty() && !inputFocused) {
            txt(g, "minecraft:block_id...", inputX + 4, inputY + 4, C_TEXT_HINT);
        } else {
            // Clip display if text too wide
            String display = inputText;
            int maxTxtW = inputW - 10;
            if (tw(display) > maxTxtW) {
                while (!display.isEmpty() && tw("..." + display) > maxTxtW) {
                    display = display.substring(1);
                }
                display = "..." + display;
            }
            txt(g, display, inputX + 4, inputY + 4, C_TEXT);

            // Blinking cursor
            if (inputFocused && (blinkTick / 20) % 2 == 0) {
                int cursorX = inputX + 4 + tw(inputText);
                if (cursorX > inputX + inputW - 6) cursorX = inputX + inputW - 6;
                fill(g, cursorX, inputY + 2, cursorX + 1, inputY + INPUT_H - 2, C_TEXT);
            }
        }

        // [+] add button
        int addX = inputX + inputW + 4;
        boolean addHov = mx >= addX && mx < addX + ADD_BTN_W
                && my >= inputY && my < inputY + INPUT_H && !dimmed;
        int addBg = addHov ? 0xFF1C1C38 : 0xFF141430;
        int addBrd = addHov ? accent() : C_BORDER;
        panel(g, addX, inputY, ADD_BTN_W, INPUT_H, addBg, addBrd);
        int plusColor = addHov ? C_ON : C_TEXT_DIM;
        txt(g, "+", addX + (ADD_BTN_W - tw("+")) / 2, inputY + 4, plusColor);

        cy += INPUT_H + INPUT_PAD;

        // ── Tag chips ──
        List<String> items = setting.get();
        if (!items.isEmpty()) {
            cy += SECTION_GAP;
            int chipStartX = x + 8;
            int contentW = w - 16;
            int cx = 0;

            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i).trim();
                if (item.isEmpty()) continue;

                int cw = chipWidth(item);

                // Wrap to next row if needed
                if (cx > 0 && cx + cw > contentW) {
                    cy += CHIP_H + CHIP_ROW_GAP;
                    cx = 0;
                }

                int chipX = chipStartX + cx;
                int chipY = cy;

                // Chip hover detection
                boolean chipHov = mx >= chipX && mx < chipX + cw
                        && my >= chipY && my < chipY + CHIP_H && !dimmed;

                // Chip background — subtle accent tinted
                int chipBg = chipHov ? 0xFF1E1E3E : 0xFF161632;
                int chipBrd = chipHov ? C_DANGER : lerp(accent(), C_BORDER, 0.6f);
                panel(g, chipX, chipY, cw, CHIP_H, chipBg, chipBrd);

                // Chip text
                txt(g, item, chipX + CHIP_PAD_X, chipY + 3, chipHov ? C_TEXT : C_TEXT_DIM);

                // [×] remove zone
                int xBtnX = chipX + cw - tw("×") - CHIP_PAD_X - 2;
                // Subtle separator line before ×
                fill(g, xBtnX - 2, chipY + 2, xBtnX - 1, chipY + CHIP_H - 2,
                        0x18FFFFFF);
                int xColor = chipHov ? C_DANGER : 0xFF555570;
                txt(g, "×", xBtnX, chipY + 3, xColor);

                cx += cw + CHIP_GAP;
            }
        }

        // ── Empty state hint ──
        if (items.isEmpty()) {
            txt(g, "Список пуст — добавьте блоки выше",
                    x + 8, cy + 2, C_TEXT_HINT);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Input handling
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected boolean onClick(double mx, double my, int btn) {
        if (btn != 0 || dimmed) return false;

        int inputX = x + 8;
        int inputW = w - 16 - ADD_BTN_W - 4;
        int inputY = y + LABEL_H + INPUT_PAD;

        // Click on input field → focus it
        if (mx >= inputX && mx < inputX + inputW
                && my >= inputY && my < inputY + INPUT_H) {
            inputFocused = true;
            return true;
        }

        // Click on [+] button → add item
        int addX = inputX + inputW + 4;
        if (mx >= addX && mx < addX + ADD_BTN_W
                && my >= inputY && my < inputY + INPUT_H) {
            addCurrentInput();
            return true;
        }

        // Click on a chip's [×] → remove it
        int cy = inputY + INPUT_H + INPUT_PAD + SECTION_GAP;
        List<String> items = setting.get();
        if (!items.isEmpty()) {
            int chipStartX = x + 8;
            int contentW = w - 16;
            int cx = 0;
            int rowY = cy;

            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i).trim();
                if (item.isEmpty()) continue;

                int cw = chipWidth(item);

                if (cx > 0 && cx + cw > contentW) {
                    rowY += CHIP_H + CHIP_ROW_GAP;
                    cx = 0;
                }

                int chipX = chipStartX + cx;
                int chipY = rowY;

                // Check if click is on this chip
                if (mx >= chipX && mx < chipX + cw
                        && my >= chipY && my < chipY + CHIP_H) {
                    // Remove this item
                    List<String> newList = new ArrayList<>(items);
                    newList.remove(i);
                    setting.set(newList);
                    recalcHeight();
                    return true;
                }

                cx += cw + CHIP_GAP;
            }
        }

        // Click elsewhere → unfocus
        inputFocused = false;
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!inputFocused || dimmed) return false;

        // Enter → add
        if (key == 257) {
            addCurrentInput();
            return true;
        }

        // Escape → unfocus
        if (key == 256) {
            inputFocused = false;
            return true;
        }

        // Backspace → delete last char
        if (key == 259 && !inputText.isEmpty()) {
            inputText = inputText.substring(0, inputText.length() - 1);
            return true;
        }

        // Ctrl+A → select all (clear)
        if (key == 65 && (mods & 2) != 0) {
            inputText = "";
            return true;
        }

        // Ctrl+V → paste from clipboard
        if (key == 86 && (mods & 2) != 0) {
            try {
                String clip = net.minecraft.client.Minecraft.getInstance()
                        .keyboardHandler.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    // Handle multi-line paste: split by commas/newlines and add all
                    String[] parts = clip.split("[,\\n\\r]+");
                    if (parts.length > 1) {
                        for (String part : parts) {
                            String trimmed = part.trim().toLowerCase();
                            if (!trimmed.isEmpty()) {
                                addItem(trimmed);
                            }
                        }
                    } else {
                        inputText += clip.trim().toLowerCase();
                    }
                    return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (!inputFocused || dimmed) return false;

        // Only allow valid block ID characters: a-z, 0-9, _, :, .
        if (ch >= 32 && isValidBlockIdChar(ch)) {
            inputText += Character.toLowerCase(ch);
            return true;
        }

        // Comma/semicolon → treat as separator, add current and continue
        if (ch == ',' || ch == ';') {
            addCurrentInput();
            return true;
        }

        return false;
    }

    private boolean isValidBlockIdChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == ':' || c == '.' || c == '-' || c == '/';
    }

    // ═══════════════════════════════════════════════════════════════
    // List manipulation
    // ═══════════════════════════════════════════════════════════════

    private void addCurrentInput() {
        String trimmed = inputText.trim().toLowerCase();
        if (!trimmed.isEmpty()) {
            addItem(trimmed);
        }
        inputText = "";
    }

    private void addItem(String item) {
        List<String> current = new ArrayList<>(setting.get());
        // Avoid duplicates
        if (!current.contains(item)) {
            current.add(item);
            setting.set(current);
            recalcHeight();
        }
    }
}
