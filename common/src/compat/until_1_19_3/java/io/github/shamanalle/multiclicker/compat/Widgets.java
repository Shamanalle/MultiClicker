package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Vanilla widget calls that were renamed or added between Minecraft versions. */
public final class Widgets {
    private Widgets() {
    }

    public static void focus(EditBox box, boolean focused) {
        box.setFocus(focused);
    }

    public static int x(AbstractWidget widget) {
        return widget.getX();
    }

    public static int y(AbstractWidget widget) {
        return widget.getY();
    }

    /** The grey text shown while the box is empty. */
    public static void hint(EditBox box, Component hint) {
        box.setHint(hint);
    }
}
