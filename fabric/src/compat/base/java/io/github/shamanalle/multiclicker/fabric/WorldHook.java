package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.matrixStack() != null && context.consumers() != null) {
                HighlightRenderer.render(context.matrixStack(), context.consumers(), context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(false));
            }
        });
    }
}
