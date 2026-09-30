package io.github.shamanalle.multiclicker.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World space box drawing. The box is already relative to the camera. */
public final class Shapes {
    private static final float LINE_WIDTH = 2.0F;

    private Shapes() {
    }

    public static Vec3 cameraPosition(Camera camera) {
        return camera.position();
    }

    public static void filledBox(PoseStack poseStack, MultiBufferSource buffers, AABB box, float r, float g, float b, float a) {
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.debugFilledBox());
        PoseStack.Pose pose = poseStack.last();
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        // The pipeline does not cull faces, so the winding order does not matter.
        quad(consumer, pose, r, g, b, a, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // bottom
        quad(consumer, pose, r, g, b, a, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // top
        quad(consumer, pose, r, g, b, a, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        quad(consumer, pose, r, g, b, a, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        quad(consumer, pose, r, g, b, a, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        quad(consumer, pose, r, g, b, a, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }

    public static void lineBox(PoseStack poseStack, MultiBufferSource buffers, AABB box, float r, float g, float b, float a) {
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.lines());
        PoseStack.Pose pose = poseStack.last();
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        // Four edges along each axis.
        for (float y : new float[]{y0, y1}) {
            for (float z : new float[]{z0, z1}) {
                line(consumer, pose, r, g, b, a, x0, y, z, x1, y, z);
            }
        }
        for (float x : new float[]{x0, x1}) {
            for (float z : new float[]{z0, z1}) {
                line(consumer, pose, r, g, b, a, x, y0, z, x, y1, z);
            }
        }
        for (float x : new float[]{x0, x1}) {
            for (float y : new float[]{y0, y1}) {
                line(consumer, pose, r, g, b, a, x, y, z0, x, y, z1);
            }
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float r, float g, float b, float a,
                             float... corners) {
        for (int i = 0; i < 12; i += 3) {
            consumer.addVertex(pose, corners[i], corners[i + 1], corners[i + 2]).setColor(r, g, b, a);
        }
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, float r, float g, float b, float a,
                             float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;
        consumer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(LINE_WIDTH);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(LINE_WIDTH);
    }
}
