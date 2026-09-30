package io.github.shamanalle.multiclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import io.github.shamanalle.multiclicker.mixin.KeyMappingAccessor;
import io.github.shamanalle.multiclicker.mixin.ToggleKeyMappingAccessor;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simulates key input through vanilla {@link KeyMapping}s, so the game processes it exactly like
 * a real press: attack cooldown, block breaking, item use and server sync all stay vanilla.
 *
 * <p>Releasing a key gives it back to the player: a normal key goes back to its physical state,
 * a key in toggle mode (toggle sprint / toggle sneak) goes back to the state it had before.</p>
 */
public final class Input {
    /** Toggle-mode keys the mod is holding, with their state before the mod took them over. */
    private static final Map<KeyMapping, Boolean> savedToggleState = new IdentityHashMap<>();

    private Input() {
    }

    /** Registers one press (like a mouse click) that vanilla consumes this tick. */
    public static void click(KeyMapping mapping) {
        KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
        accessor.multiclicker$setClickCount(accessor.multiclicker$getClickCount() + 1);
        hold(mapping);
    }

    /** Keeps the key held down (for continuous actions such as mining, walking or eating). */
    public static void hold(KeyMapping mapping) {
        if (isToggleMode(mapping)) {
            savedToggleState.putIfAbsent(mapping, mapping.isDown());
        }
        // Written directly: ToggleKeyMapping.setDown(true) would flip the state on every call.
        ((KeyMappingAccessor) mapping).multiclicker$setDown(true);
    }

    /** Releases a simulated key, but keeps it down if the player is holding (or toggled) it. */
    public static void release(KeyMapping mapping) {
        Boolean saved = savedToggleState.remove(mapping);
        if (saved != null) {
            ((KeyMappingAccessor) mapping).multiclicker$setDown(saved);
        } else if (!isToggleMode(mapping)) {
            ((KeyMappingAccessor) mapping).multiclicker$setDown(isPhysicallyDown(mapping));
        }
    }

    private static boolean isToggleMode(KeyMapping mapping) {
        return mapping instanceof ToggleKeyMapping
                && ((ToggleKeyMappingAccessor) mapping).multiclicker$getNeedsToggle().getAsBoolean();
    }

    public static boolean isPhysicallyDown(KeyMapping mapping) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return false;
        }
        InputConstants.Key key = ((KeyMappingAccessor) mapping).multiclicker$getKey();
        return switch (key.getType()) {
            case MOUSE -> switch (key.getValue()) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> mc.mouseHandler.isLeftPressed();
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> mc.mouseHandler.isRightPressed();
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> mc.mouseHandler.isMiddlePressed();
                default -> GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), key.getValue()) == GLFW.GLFW_PRESS;
            };
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue()
                    && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
            case SCANCODE -> false;
        };
    }

    /** True while the player is steering the character with movement keys. */
    public static boolean isPlayerMoving(Minecraft mc) {
        return isPhysicallyDown(mc.options.keyUp) || isPhysicallyDown(mc.options.keyDown)
                || isPhysicallyDown(mc.options.keyLeft) || isPhysicallyDown(mc.options.keyRight)
                || isPhysicallyDown(mc.options.keyJump);
    }

    /**
     * Uses the item in a hand the way vanilla does when the crosshair points at nothing: the item
     * itself is used (eat, cast a rod...) and the block or entity under the crosshair is never
     * touched, so eating in front of a chest does not open it and a carrot is not fed to a pig.
     *
     * @return {@code true} if the item was used
     */
    public static boolean useItem(Minecraft mc, InteractionHand hand) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || mc.gameMode.isDestroying() || player.isHandsBusy()
                || player.isUsingItem()) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.isItemEnabled(mc.level.enabledFeatures())) {
            return false;
        }
        if (mc.gameMode.useItem(player, hand) instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                player.swing(hand);
            }
            mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
            return true;
        }
        return false;
    }
}
