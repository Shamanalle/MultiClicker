package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World space box drawing. The box is already relative to the camera. */
public final class Shapes {
    private Shapes() {
    }

    /** Where the shapes of one frame are drawn. */
    public static final class Sink {
        final PoseStack poseStack;
        final MultiBufferSource buffers;

        public Sink(PoseStack poseStack, MultiBufferSource buffers) {
            this.poseStack = poseStack;
            this.buffers = buffers;
        }
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.getPosition();
    }

    public static void filledBox(Sink sink, AABB box, float r, float g, float b, float a) {
        LevelRenderer.addChainedFilledBoxVertices(sink.poseStack, sink.buffers.getBuffer(RenderType.debugFilledBox()),
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    public static void lineBox(Sink sink, AABB box, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(sink.poseStack, sink.buffers.getBuffer(RenderType.lines()), box, r, g, b, a);
    }
}
