package io.github.shamanalle.multiclicker.forge;

import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;

/** Draws the HUD as a layer above the vanilla ones. */
final class HudHook {
    private HudHook() {
    }

    static void register(IEventBus modBus, MultiClicker mod) {
        modBus.addListener((AddGuiOverlayLayersEvent event) ->
                event.getLayeredDraw().add((graphics, partialTick) -> mod.hud().render(new Canvas(graphics))));
    }
}
