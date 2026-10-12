package io.github.shamanalle.multiclicker.forge;

import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;

/** Draws the HUD as an overlay above the vanilla ones. */
final class HudHook {
    private HudHook() {
    }

    static void register(IEventBus modBus, MultiClicker mod) {
        modBus.addListener((RegisterGuiOverlaysEvent event) ->
                event.registerAboveAll("hud", (gui, graphics, partialTick, width, height) -> mod.hud().render(new Canvas(graphics))));
    }
}
