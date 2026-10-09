package io.github.shamanalle.multiclicker.gui;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import io.github.shamanalle.multiclicker.compat.Keys;

import java.util.function.Consumer;

/**
 * Waits for the player to press the key for a hotkey. Escape cancels, Backspace or Delete removes
 * the hotkey, and the middle and side mouse buttons can be bound too.
 */
public final class KeyCapture {
    @Nullable
    private Object target;
    @Nullable
    private Consumer<InputConstants.Key> onKey;
    /** The key just bound may also arrive as a typed character, which must not reach a text field. */
    private boolean swallowChar;

    /** Starts waiting for a key for the given target (a setting or a profile name). */
    public void start(Object target, Consumer<InputConstants.Key> onKey) {
        this.target = target;
        this.onKey = onKey;
    }

    public boolean isActive() {
        return target != null;
    }

    public boolean isWaitingFor(Object candidate) {
        return target != null && target.equals(candidate);
    }

    public void cancel() {
        target = null;
        onKey = null;
    }

    /** Handles a key press while waiting; returns whether it was used. */
    public boolean keyPressed(int keyCode) {
        if (onKey == null) {
            swallowChar = false;
            return false;
        }
        swallowChar = true;
        if (keyCode != InputConstants.KEY_ESCAPE) {
            boolean clear = keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE;
            onKey.accept(clear ? InputConstants.UNKNOWN : Keys.keyboard(keyCode));
        }
        cancel();
        return true;
    }

    /** Whether a typed character belongs to the key that was just bound (and is to be ignored). */
    public boolean swallowTyped() {
        boolean swallow = swallowChar || isActive();
        swallowChar = false;
        return swallow;
    }

    /**
     * Handles a mouse click while waiting: the middle and side buttons are bound, a left or right
     * click (needed to use the menu) cancels.
     */
    public boolean mouseClicked(int button) {
        if (onKey == null) {
            return false;
        }
        if (button >= 2) {
            onKey.accept(Keys.mouse(button));
        }
        cancel();
        return true;
    }
}
