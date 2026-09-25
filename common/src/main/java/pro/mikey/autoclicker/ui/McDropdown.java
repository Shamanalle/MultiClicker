package pro.mikey.autoclicker.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pro.mikey.autoclicker.core.Setting;

public class McDropdown extends McWidget {

    private final Setting.EnumSetting<?> setting;

    public McDropdown(int x, int y, int w, int h, Setting.EnumSetting<?> setting) {
        super(x, y, w, h, setting.getDisplayName());
        this.setting = setting;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int mx, int my, float dt) {
        if (hoverAnim > 0.01f) fill(g, x, y, x + w, y + h, (int)(hoverAnim * 15) << 24 | 0x00FFFFFF);

        int ly = subtitle != null ? y + 5 : y + (h - 9) / 2;
        txts(g, label, x + 8, ly, C_TEXT);
        if (subtitle != null) drawSub(g, x + 8, ly);

        String val = setting.get().toString();
        int vw = tw(val), pw = vw + 30, px = x + w - pw - 10, py = y + (h - 16) / 2;
        boolean vh = mx >= px && mx < px + pw && my >= py && my < py + 16;
        panel(g, px, py, pw, 16, vh ? 0xFF1C1C38 : 0xFF141430, vh ? accent() : C_BORDER);
        txt(g, "<", px + 5, py + 4, vh ? C_TEXT : C_TEXT_DIM);
        txt(g, val, px + (pw - vw) / 2, py + 4, C_TEXT);
        txt(g, ">", px + pw - 11, py + 4, vh ? C_TEXT : C_TEXT_DIM);
    }

    @Override
    protected boolean onClick(double mx, double my, int btn) {
        if (btn == 0) { setting.cycle(); return true; }
        if (btn == 1) { setting.cycleBack(); return true; }
        return false;
    }
}
