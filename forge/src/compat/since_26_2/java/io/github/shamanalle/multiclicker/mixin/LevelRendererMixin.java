package io.github.shamanalle.multiclicker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/**
 * Forge for Minecraft 1.21.2 and newer has no world rendering event, so the outline and filled highlight styles
 * are submitted right after the entities.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void multiclicker$renderHighlight(PoseStack poseStack, LevelRenderState levelState, SubmitNodeCollector collector,
            CallbackInfo ci) {
        HighlightRenderer.render(new Shapes.Sink(poseStack, collector), levelState.cameraRenderState.pos,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
