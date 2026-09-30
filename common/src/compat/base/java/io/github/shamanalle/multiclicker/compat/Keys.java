package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Keyboard state and key bindings. */
public final class Keys {
    private Keys() {
    }

    public static boolean isKeyDown(Minecraft mc, int key) {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key);
    }

    public static boolean isMouseDown(Minecraft mc, int button) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
    }

    public static boolean shiftDown() {
        Minecraft mc = Minecraft.getInstance();
        return isKeyDown(mc, GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(mc, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** Ctrl, or Cmd on macOS. */
    public static boolean controlDown() {
        Minecraft mc = Minecraft.getInstance();
        boolean mac = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
        return isKeyDown(mc, mac ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL)
                || isKeyDown(mc, mac ? GLFW.GLFW_KEY_RIGHT_SUPER : GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    /** A key binding of this mod. */
    public static KeyMapping keyMapping(String name, int key) {
        return new KeyMapping(name, key, "key.categories.multiclicker");
    }
}
