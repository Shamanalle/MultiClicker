package pro.mikey.autoclicker.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.visual.HighlightModule;

/** Draws the outline / filled box around the current target (the glow style is done by a mixin). */
public final class HighlightRenderer {
    private HighlightRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        MultiClicker mod = MultiClicker.get();
        if (mod == null) {
            return;
        }
        HighlightModule highlight = mod.highlight();
        if (highlight.style.get() == HighlightModule.Style.GLOW) {
            return;
        }
        Entity target = highlight.target(Minecraft.getInstance());
        if (target == null) {
            return;
        }

        // Interpolate between ticks so the box follows the entity smoothly, then make it camera-relative.
        Vec3 offset = target.getPosition(partialTick).subtract(target.position()).subtract(camera.getPosition());
        AABB box = target.getBoundingBox().move(offset).inflate(0.05);

        int color = highlight.color();
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        if (highlight.style.get() == HighlightModule.Style.FILLED) {
            ShapeRenderer.addChainedFilledBoxVertices(poseStack, buffers.getBuffer(RenderType.debugFilledBox()),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.22F);
        }
        ShapeRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box, r, g, b, 1.0F);
    }
}
