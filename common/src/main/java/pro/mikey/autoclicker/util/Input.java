package pro.mikey.autoclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import pro.mikey.autoclicker.mixin.KeyMappingAccessor;

/**
 * Simulates key input through vanilla {@link KeyMapping}s, so the game processes it exactly like
 * a real press: attack cooldown, block breaking, item use and server sync all stay vanilla.
 */
public final class Input {
    private Input() {
    }

    /** Registers one press (like a mouse click) that vanilla consumes this tick. */
    public static void click(KeyMapping mapping) {
        KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
        accessor.multiclicker$setClickCount(accessor.multiclicker$getClickCount() + 1);
        mapping.setDown(true);
    }

    /** Keeps the key held down (for continuous actions such as mining or eating). */
    public static void hold(KeyMapping mapping) {
        mapping.setDown(true);
    }

    /** Releases a simulated key, but keeps it down if the player is physically holding it. */
    public static void release(KeyMapping mapping) {
        mapping.setDown(isPhysicallyDown(mapping));
    }

    public static boolean isPhysicallyDown(KeyMapping mapping) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return false;
        }
        InputConstants.Key key = ((KeyMappingAccessor) mapping).multiclicker$getKey();
        long window = mc.getWindow().getWindow();
        return switch (key.getType()) {
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue()
                    && InputConstants.isKeyDown(window, key.getValue());
            case SCANCODE -> false;
        };
    }

    /** True while the player is steering the character with movement keys. */
    public static boolean isPlayerMoving(Minecraft mc) {
        return isPhysicallyDown(mc.options.keyUp) || isPhysicallyDown(mc.options.keyDown)
                || isPhysicallyDown(mc.options.keyLeft) || isPhysicallyDown(mc.options.keyRight)
                || isPhysicallyDown(mc.options.keyJump);
    }
}
