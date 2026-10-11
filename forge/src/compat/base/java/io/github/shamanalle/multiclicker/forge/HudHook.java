package io.github.shamanalle.multiclicker.forge;

import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Ids;

/** Draws the HUD as a layer above the vanilla ones. */
final class HudHook {
    private HudHook() {
    }

    static void register(BusGroup modBus, MultiClicker mod) {
        AddGuiOverlayLayersEvent.getBus(modBus).addListener(event ->
                event.getLayeredDraw().add(Ids.mod("hud"), (graphics, deltaTracker) -> mod.hud().render(new Canvas(graphics))));
    }
}
