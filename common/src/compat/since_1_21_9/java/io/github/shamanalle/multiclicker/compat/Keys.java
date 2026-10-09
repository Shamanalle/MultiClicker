package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Keyboard state and key bindings. */
public final class Keys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Ids.mod("main"));

    private Keys() {
    }

    public static boolean isKeyDown(Minecraft mc, int key) {
        return InputConstants.isKeyDown(mc.getWindow(), key);
    }

    public static boolean isMouseDown(Minecraft mc, int button) {
        return GLFW.glfwGetMouseButton(mc.getWindow().handle(), button) == GLFW.GLFW_PRESS;
    }

    /** Whether the player physically holds the key or mouse button (simulated presses do not count). */
    public static boolean isDown(Minecraft mc, InputConstants.Key key) {
        return switch (key.getType()) {
            case MOUSE -> switch (key.getValue()) {
                case InputConstants.MOUSE_BUTTON_LEFT -> mc.mouseHandler.isLeftPressed();
                case InputConstants.MOUSE_BUTTON_RIGHT -> mc.mouseHandler.isRightPressed();
                case InputConstants.MOUSE_BUTTON_MIDDLE -> mc.mouseHandler.isMiddlePressed();
                default -> isMouseDown(mc, key.getValue());
            };
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue() && isKeyDown(mc, key.getValue());
            case SCANCODE -> false;
        };
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

    /** A keyboard key by its GLFW key code. */
    public static InputConstants.Key keyboard(int keyCode) {
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
    }

    /** A mouse button (0 = left, 1 = right, 2 = middle, 3+ = side buttons). */
    public static InputConstants.Key mouse(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

    /** Reads a saved key name such as {@code key.keyboard.g}; unknown names give {@link InputConstants#UNKNOWN}. */
    public static InputConstants.Key parse(String name) {
        if (name == null || name.isEmpty()) {
            return InputConstants.UNKNOWN;
        }
        try {
            return InputConstants.getKey(name);
        } catch (IllegalArgumentException e) {
            return InputConstants.UNKNOWN;
        }
    }

    /** A key binding of this mod. */
    public static KeyMapping keyMapping(String name, int key) {
        return new KeyMapping(name, key, CATEGORY);
    }
}
