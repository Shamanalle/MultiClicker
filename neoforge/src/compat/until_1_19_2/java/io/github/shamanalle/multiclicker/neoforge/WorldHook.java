package io.github.shamanalle.multiclicker.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            // Forge for 1.19.2 and older has no stage right after the entities.
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                Minecraft mc = Minecraft.getInstance();
                HighlightRenderer.render(new Shapes.Sink(event.getPoseStack(), mc.renderBuffers().bufferSource()), Shapes.cameraPosition(event.getCamera()),
                        event.getPartialTick());
                mc.renderBuffers().bufferSource().endBatch();
            }
        });
    }
}
