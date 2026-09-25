package pro.mikey.autoclicker.core;

import net.minecraft.client.Minecraft;

/**
 * Base contract for every mod feature.
 *
 * Each Module owns its state, its config slice, and its UI section.
 * The ModuleManager dispatches lifecycle and tick events.
 */
public interface Module {

    // ── Identity ──────────────────────────────────────────────────────

    /** Unique ID used as config key (e.g. "anti_afk", "auto_eat"). */
    String getId();

    /** Human-readable name for the UI (e.g. "Анти-АФК"). */
    String getDisplayName();

    /** Category for UI tab grouping. */
    Category getCategory();

    // ── Lifecycle ─────────────────────────────────────────────────────

    /** Called once when the mod initializes (register keybinds, load config). */
    default void onInit(Minecraft mc) {
    }

    /** Called when the global toggle is activated (I key). */
    default void onEnable() {
    }

    /** Called when the global toggle is deactivated. Must cleanup state. */
    default void onDisable() {
    }

    // ── Per-tick logic ────────────────────────────────────────────────

    /**
     * Called every game tick (20 TPS) while the mod is active.
     * Return true to consume the tick and prevent lower-priority modules
     * from running (replaces the old "return;" pattern in clientTickEvent).
     */
    default boolean onTick(Minecraft mc) {
        return false;
    }

    /** Priority: lower value = runs first. Default 100. */
    default int tickPriority() {
        return 100;
    }

    // ── Rendering ────────────────────────────────────────────────────

    /** Called every render frame for HUD overlay drawing. */
    default void onHudRender(Minecraft mc, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker delta) {
    }

    /** Called every render frame for 3D world rendering (ESP, etc.). */
    default void onWorldRender(Minecraft mc, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack) {
    }

    /**
     * Returns all configurable settings for this module.
     * Used by ConfigManager for serialization and by the UI for auto-generation.
     */
    default java.util.List<Setting<?>> getSettings() {
        return java.util.List.of();
    }

    // ── Keybinding ─────────────────────────────────────────────────

    /** Returns the GLFW key code bound to this module, or -1 for none. */
    default int getKeyBind() {
        return -1;
    }

    /** Sets the GLFW key code for this module's toggle bind. */
    default void setKeyBind(int keyCode) {
    }

    /** Returns a human-readable name for the current key bind. */
    default String getKeyBindName() {
        int key = getKeyBind();
        if (key <= 0) return "NONE";
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "KEY_" + key;
    }

    // ── Category enum ────────────────────────────────────────────────

    enum Category {
        CLICKER("\u041a\u043b\u0438\u043a\u0435\u0440", "\uD83D\uDDB1", 0xFFFFAA55),
        COMBAT("\u0411\u043e\u0439", "\u2694", 0xFFFF5555),
        PROTECTION("\u0417\u0430\u0449\u0438\u0442\u0430", "\uD83D\uDEE1", 0xFF55AAFF),
        AUTOMATION("\u0410\u0432\u0442\u043e", "\u2699", 0xFF55DD66),
        WORLD("\u041c\u0438\u0440", "\uD83C\uDF0D", 0xFF44DDAA),
        RENDER("\u0412\u0438\u0437\u0443\u0430\u043b", "\uD83D\uDC41", 0xFF9966FF);

        public final String displayName;
        public final String icon;
        public final int color;

        Category(String displayName, String icon, int color) {
            this.displayName = displayName;
            this.icon = icon;
            this.color = color;
        }
    }
}
