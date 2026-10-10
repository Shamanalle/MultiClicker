package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Ids;

/** Registers the HUD panel. */
final class HudHook {
    private HudHook() {
    }

    static void register(MultiClicker mod) {
        HudElementRegistry.addLast(Ids.mod("hud"),
                (graphics, deltaTracker) -> mod.hud().render(new Canvas(graphics)));
    }
}
