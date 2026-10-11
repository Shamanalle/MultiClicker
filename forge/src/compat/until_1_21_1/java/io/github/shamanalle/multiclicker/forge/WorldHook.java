package io.github.shamanalle.multiclicker.forge;

import com.mojang.blaze3d.vertex.PoseStack;
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
                // Forge passes the camera rotation as a bare matrix since 1.20.5.
                PoseStack pose = new PoseStack();
                pose.mulPose(event.getPoseStack());
                HighlightRenderer.render(new Shapes.Sink(pose, mc.renderBuffers().bufferSource()), Shapes.cameraPosition(event.getCamera()),
                        event.getPartialTick());
            }
        });
    }
}
