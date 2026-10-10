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
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                Minecraft mc = Minecraft.getInstance();
                HighlightRenderer.render(new Shapes.Sink(event.getPoseStack(), mc.renderBuffers().bufferSource()), Shapes.cameraPosition(event.getCamera()),
                        event.getPartialTick());
            }
        });
    }
}
