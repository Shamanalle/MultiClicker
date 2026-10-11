package io.github.shamanalle.multiclicker.forge;

import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;

/**
 * Draws the HUD. Forge 51 (Minecraft 1.21) has no event to add a layer, so it draws when the chat layer does,
 * which is on every frame the HUD is shown while the chat is closed.
 */
final class HudHook {
    private HudHook() {
    }

    static void register(IEventBus modBus, MultiClicker mod) {
        MinecraftForge.EVENT_BUS.addListener((CustomizeGuiOverlayEvent.Chat event) -> mod.hud().render(new Canvas(event.getGuiGraphics())));
    }
}
