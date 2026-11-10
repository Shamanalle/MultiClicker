package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;

/** Keyboard state and key bindings. */
public final class Keys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Ids.mod("main"));
    private static final int MAX_SDL_SCANCODE = 512;

    private Keys() {
    }

    /** Whether the player physically holds the key or mouse button (simulated presses do not count). */
    public static boolean isDown(Minecraft mc, InputConstants.Key key) {
        int code = key.getValue();
        return switch (key.getType()) {
            case MOUSE -> switch (code) {
                case InputConstants.MOUSE_BUTTON_LEFT -> mc.mouseHandler.isLeftPressed();
                case InputConstants.MOUSE_BUTTON_RIGHT -> mc.mouseHandler.isRightPressed();
                case InputConstants.MOUSE_BUTTON_MIDDLE -> mc.mouseHandler.isMiddlePressed();
                default -> code > 0 && code <= 32 && (SDLMouse.SDL_GetMouseState(null, null) & (1 << (code - 1))) != 0;
            };
            case KEYBOARD -> code > 0 && code < MAX_SDL_SCANCODE && InputConstants.isKeyDown(code);
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

    /** A keyboard key by its key code. */
    public static InputConstants.Key keyboard(int keyCode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
    }

    /** A mouse button (0 = left, 1 = right, 2 = middle, 3+ = side buttons). */
    public static InputConstants.Key mouse(int button) {
        int sdlButton = switch (button) {
            case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
            case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
            case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> button + 1;
        };
        return InputConstants.Type.MOUSE.getOrCreate(sdlButton);
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

    /** Flashes the taskbar or dock icon when the game window is in the background. */
    public static void requestAttention(Minecraft mc) {
        if (!mc.isWindowActive()) {
            long handle = mc.getWindow().handle();
            if (handle != 0L) {
                SDLVideo.SDL_FlashWindow(handle, SDLVideo.SDL_FLASH_BRIEFLY);
            }
        }
    }
}
