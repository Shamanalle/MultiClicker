package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Keyboard state and key bindings. */
public final class Keys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Ids.mod("main"));

    private Keys() {
    }

    /** Whether the player physically holds the key or mouse button (simulated presses do not count). */
    public static boolean isDown(Minecraft mc, InputConstants.Key key) {
        return switch (key.getType()) {
            case MOUSE -> switch (key.getValue()) {
                case InputConstants.MOUSE_BUTTON_LEFT -> mc.mouseHandler.isLeftPressed();
                case InputConstants.MOUSE_BUTTON_RIGHT -> mc.mouseHandler.isRightPressed();
                case InputConstants.MOUSE_BUTTON_MIDDLE -> mc.mouseHandler.isMiddlePressed();
                default -> false;
            };
            case KEYBOARD -> key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(key.getValue());
        };
    }

    public static boolean shiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    /** Ctrl, or Cmd on macOS. */
    public static boolean controlDown() {
        boolean mac = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
        return InputConstants.isKeyDown(mac ? InputConstants.KEY_LGUI : InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(mac ? InputConstants.KEY_RGUI : InputConstants.KEY_RCONTROL);
    }

    /** A key binding of this mod. */
    public static KeyMapping keyMapping(String name, int key) {
        return new KeyMapping(name, key, CATEGORY);
    }
}
