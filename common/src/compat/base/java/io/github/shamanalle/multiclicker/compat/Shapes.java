package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World space box drawing. The box is already relative to the camera. */
public final class Shapes {
    private Shapes() {
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.getPosition();
    }

    public static void filledBox(PoseStack poseStack, MultiBufferSource buffers, AABB box, float r, float g, float b, float a) {
        ShapeRenderer.addChainedFilledBoxVertices(poseStack, buffers.getBuffer(RenderType.debugFilledBox()),
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    public static void lineBox(PoseStack poseStack, MultiBufferSource buffers, AABB box, float r, float g, float b, float a) {
        ShapeRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box, r, g, b, a);
    }
}
