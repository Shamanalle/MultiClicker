package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            HighlightRenderer.render(new Shapes.Sink(context.poseStack(), context.submitNodeCollector()), context.levelState().cameraRenderState.pos,
                    mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        });
    }
}
