package io.github.shamanalle.multiclicker.forge;

import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;

import java.util.function.Consumer;

/**
 * Draws the HUD. Forge for Minecraft 1.21.6 and 1.21.7 has no event to add a layer, so it draws when the chat
 * layer does, which is on every frame the HUD is shown while the chat is closed.
 */
final class HudHook {
    private HudHook() {
    }

    static void register(BusGroup modBus, MultiClicker mod) {
        // A Consumer: the bus also takes a Predicate, which would cancel the chat.
        Consumer<CustomizeGuiOverlayEvent.Chat> draw = event -> mod.hud().render(new Canvas(event.getGuiGraphics()));
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(draw);
    }
}
