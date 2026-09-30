package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;

/** Registers the HUD panel. */
final class HudHook {
    private HudHook() {
    }

    static void register(MultiClicker mod) {
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> mod.hud().render(new Canvas(graphics), deltaTracker));
    }
}
