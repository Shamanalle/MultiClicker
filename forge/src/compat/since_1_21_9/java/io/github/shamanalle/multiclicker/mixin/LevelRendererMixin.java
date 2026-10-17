package io.github.shamanalle.multiclicker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/**
 * Forge for Minecraft 1.21.2 and newer has no world rendering event, so the outline and filled highlight styles
 * are drawn at the first pose stack check of a frame, which comes right after the entities are drawn.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique
    private boolean multiclicker$highlightDrawn;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void multiclicker$newFrame(CallbackInfo ci) {
        multiclicker$highlightDrawn = false;
    }

    @Inject(method = "checkPoseStack", at = @At("HEAD"))
    private void multiclicker$renderHighlight(PoseStack poseStack, CallbackInfo ci) {
        if (!multiclicker$highlightDrawn) {
            multiclicker$highlightDrawn = true;
            Minecraft mc = Minecraft.getInstance();
            HighlightRenderer.render(new Shapes.Sink(poseStack, mc.renderBuffers().bufferSource()),
                    Shapes.cameraPosition(mc.gameRenderer.getMainCamera()), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }
    }
}
