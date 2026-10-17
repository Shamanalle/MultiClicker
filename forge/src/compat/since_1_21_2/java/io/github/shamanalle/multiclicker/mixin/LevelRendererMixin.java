package io.github.shamanalle.multiclicker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

import java.util.List;

/**
 * Forge for Minecraft 1.21.2 and newer has no world rendering event, so the outline and filled highlight styles
 * are drawn right after the entities, where Fabric and NeoForge draw them too.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void multiclicker$renderHighlight(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera,
            DeltaTracker deltaTracker, List<Entity> entities, CallbackInfo ci) {
        HighlightRenderer.render(new Shapes.Sink(poseStack, bufferSource), Shapes.cameraPosition(camera),
                deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
