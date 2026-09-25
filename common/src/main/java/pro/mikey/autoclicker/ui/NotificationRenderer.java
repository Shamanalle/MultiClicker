package pro.mikey.autoclicker.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NotificationRenderer — singleton that manages in-game toast notifications.
 * Notifications appear in the bottom-right corner, stack upwards, and fade out
 * after 3 seconds.
 * 
 * Usage:
 * NotificationRenderer.show("Module Enabled", "AutoEat is now active",
 * 0xFF44CC66);
 */
public class NotificationRenderer {

    private static final NotificationRenderer INSTANCE = new NotificationRenderer();
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    private static final int DISPLAY_TIME_MS = 3000;
    private static final int FADE_TIME_MS = 500;
    private static final int MAX_VISIBLE = 5;
    private static final int NOTIF_W = 180;
    private static final int NOTIF_H = 32;
    private static final int NOTIF_GAP = 4;
    private static final int MARGIN = 8;

    public static NotificationRenderer getInstance() {
        return INSTANCE;
    }

    /** Show a notification toast. */
    public static void show(String title, String text, int color) {
        INSTANCE.addNotification(title, text, color);
    }

    /** Convenience: module enabled notification. */
    public static void showEnabled(String moduleName) {
        show("Включён", moduleName, 0xFF44CC66);
    }

    /** Convenience: module disabled notification. */
    public static void showDisabled(String moduleName) {
        show("Выключен", moduleName, 0xFFCC4444);
    }

    /** Convenience: warning notification. */
    public static void showWarning(String text) {
        show("⚠ Внимание", text, 0xFFFFAA33);
    }

    private void addNotification(String title, String text, int color) {
        notifications.add(new Notification(title, text, color, System.currentTimeMillis()));
        // Limit max queue
        while (notifications.size() > MAX_VISIBLE * 2) {
            notifications.remove(0);
        }
    }

    /** Called from HUD render event every frame. */
    public void render(GuiGraphics gfx) {
        if (notifications.isEmpty())
            return;

        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        // Remove expired notifications (CopyOnWriteArrayList-safe)
        notifications.removeIf(n -> now - n.showTime > DISPLAY_TIME_MS + FADE_TIME_MS);

        // Render from bottom up (newest at bottom)
        int visibleCount = 0;
        for (int i = notifications.size() - 1; i >= 0 && visibleCount < MAX_VISIBLE; i--) {
            Notification n = notifications.get(i);
            long elapsed = now - n.showTime;

            // Calculate alpha for animation
            float alpha;
            if (elapsed < 200) {
                // Slide in
                alpha = elapsed / 200f;
            } else if (elapsed > DISPLAY_TIME_MS) {
                // Fade out
                alpha = 1.0f - (float) (elapsed - DISPLAY_TIME_MS) / FADE_TIME_MS;
            } else {
                alpha = 1.0f;
            }
            alpha = Math.max(0, Math.min(1, alpha));

            int nx = screenW - NOTIF_W - MARGIN;
            int ny = screenH - MARGIN - NOTIF_H - (NOTIF_H + NOTIF_GAP) * visibleCount;

            // Slide from right
            float slideOffset = (1.0f - alpha) * 40;
            nx += (int) slideOffset;

            int bgAlpha = (int) (alpha * 0xD0);
            int bgColor = (bgAlpha << 24) | 0x00141420;
            McWidget.fill(gfx, nx, ny, nx + NOTIF_W, ny + NOTIF_H, bgColor);

            // Accent bar (left side)
            int barAlpha = (int) (alpha * 0xFF);
            int barColor = (barAlpha << 24) | (n.color & 0x00FFFFFF);
            McWidget.fill(gfx, nx, ny, nx + 3, ny + NOTIF_H, barColor);

            // Title
            int titleAlpha = (int) (alpha * 0xFF);
            int titleColor = (titleAlpha << 24) | (n.color & 0x00FFFFFF);
            gfx.drawString(mc.font, n.title, nx + 6, ny + 4, titleColor, false);

            // Text
            int textAlpha = (int) (alpha * 0xCC);
            int textColor = (textAlpha << 24) | 0x00CCCCDD;
            gfx.drawString(mc.font, n.text, nx + 6, ny + 16, textColor, false);

            // Bottom separator
            int sepAlpha = (int) (alpha * 0x20);
            McWidget.fill(gfx, nx + 3, ny + NOTIF_H - 1, nx + NOTIF_W, ny + NOTIF_H,
                    (sepAlpha << 24) | 0x00FFFFFF);

            visibleCount++;
        }
    }

    private static class Notification {
        final String title;
        final String text;
        final int color;
        final long showTime;

        Notification(String title, String text, int color, long showTime) {
            this.title = title;
            this.text = text;
            this.color = color;
            this.showTime = showTime;
        }
    }
}
