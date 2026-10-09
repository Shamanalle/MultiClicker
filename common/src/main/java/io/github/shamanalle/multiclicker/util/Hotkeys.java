package io.github.shamanalle.multiclicker.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import io.github.shamanalle.multiclicker.compat.Keys;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Collects key and mouse button presses made while playing (vanilla reports them through
 * {@code KeyMapping.click}, which is not called while a screen such as the chat is open).
 * Presses are queued and handled at the start of the next client tick, so a short tap between two
 * ticks is never missed, and the auto-repeat of a held key does not count as a new press.
 */
public final class Hotkeys {
    private static final Set<InputConstants.Key> held = new HashSet<>();
    private static final Queue<InputConstants.Key> pressed = new ArrayDeque<>();

    private Hotkeys() {
    }

    public static void onPress(InputConstants.Key key) {
        if (!key.equals(InputConstants.UNKNOWN) && held.add(key)) {
            pressed.add(key);
        }
    }

    public static void onRelease(InputConstants.Key key) {
        held.remove(key);
    }

    /** The presses since the last call, oldest first. Keys released while the window was in the background are forgotten. */
    public static Queue<InputConstants.Key> drain(Minecraft mc) {
        held.removeIf(key -> !Keys.isDown(mc, key));
        Queue<InputConstants.Key> result = new ArrayDeque<>(pressed);
        pressed.clear();
        return result;
    }
}
