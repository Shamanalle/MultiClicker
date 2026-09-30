package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            HighlightRenderer.render(context.poseStack(), context.bufferSource(), mc.gameRenderer.getMainCamera(),
                    mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        });
    }
}
