package io.github.shamanalle.multiclicker.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterEntities event) -> {
            Minecraft mc = Minecraft.getInstance();
            HighlightRenderer.render(new Shapes.Sink(event.getPoseStack(), mc.renderBuffers().bufferSource()), Shapes.cameraPosition(event.getCamera()),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        });
    }
}
