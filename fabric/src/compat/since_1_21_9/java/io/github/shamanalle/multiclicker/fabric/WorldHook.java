package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (context.matrices() != null && context.consumers() != null) {
                HighlightRenderer.render(context.matrices(), context.consumers(), mc.gameRenderer.getMainCamera(),
                        mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            }
        });
    }
}
