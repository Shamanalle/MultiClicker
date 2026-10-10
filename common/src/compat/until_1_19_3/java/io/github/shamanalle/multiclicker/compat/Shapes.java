package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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

    /** Before Minecraft 1.19.4 there is no filled box render type: the six faces are drawn as quads (blended additively). */
    public static void filledBox(Sink sink, AABB box, float r, float g, float b, float a) {
        VertexConsumer buffer = sink.buffers.getBuffer(RenderType.lightning());
        Matrix4f m = sink.poseStack.last().pose();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        float[][] faces = {
                {x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2},
                {x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1},
                {x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1},
                {x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2},
                {x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1},
                {x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2},
        };
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                buffer.vertex(m, face[i], face[i + 1], face[i + 2]).color(r, g, b, a).endVertex();
            }
        }
    }

    public static void lineBox(Sink sink, AABB box, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(sink.poseStack, sink.buffers.getBuffer(RenderType.lines()), box, r, g, b, a);
    }
}
