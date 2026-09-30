package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (context.matrices() != null && context.consumers() != null) {
                HighlightRenderer.render(new Shapes.Sink(context.matrices(), context.consumers()), Shapes.cameraPosition(mc.gameRenderer.getMainCamera()),
                        mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            }
        });
    }
}
