package pro.mikey.autoclicker.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.world.HudModule;
import pro.mikey.autoclicker.ui.NotificationRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Fabric client entrypoint. Registers:
 * - Keybindings
 * - ClientPlayConnectionEvents for init
 * - HudRenderCallback → HudModule
 * - WorldRenderEvents → ESP rendering via HudModule settings
 */
public class AutoClickerFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(AutoClicker.openConfig);
        KeyBindingHelper.registerKeyBinding(AutoClicker.toggleHolding);

        AutoClicker instance = new AutoClicker();
        instance.onInitialize();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> instance.clientReady(client));
        });

        // HUD overlay
        HudRenderCallback.EVENT.register((context, delta) -> {
            instance.getModuleManager().<HudModule>get("hud")
                    .ifPresent(hud -> hud.render(context, delta));
            // #16 Notifications
            NotificationRenderer.getInstance().render(context);
        });

        // 3D ESP via WorldRenderEvents
        WorldRenderEvents.LAST.register(context -> {
            HudModule hud = instance.getModuleManager()
                    .<HudModule>get("hud").orElse(null);
            if (hud == null)
                return;

            HudModule.EspMode espMode = hud.espMode.get();
            if (espMode == HudModule.EspMode.OFF)
                return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null)
                return;

            // Find target — works with or without autoclick active
            CombatClickerModule clicker = instance.getModuleManager()
                    .<CombatClickerModule>get("combat_clicker").orElse(null);

            Entity target = null;
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                if (e != null && e.isAlive())
                    target = e;
            }
            if (target == null && clicker != null)
                target = clicker.getLastTarget();

            // Update espTarget for MixinEntity GLOW mode
            if (clicker != null) {
                clicker.setEspTarget((target != null && target.isAlive()) ? target : null);
            }

            if (target == null || !target.isAlive())
                return;

            // GLOW is handled by MixinEntity — espTarget is set above, no 3D render needed
            if (espMode == HudModule.EspMode.GLOW)
                return;

            // Extract espColor as RGB floats (#13)
            int color = hud.espColor.get().rgb;
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // Interpolated position
            float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);
            net.minecraft.world.phys.Vec3 camPos = context.camera().getPosition();

            double lerpedX = net.minecraft.util.Mth.lerp(partialTick, target.xo, target.getX());
            double lerpedY = net.minecraft.util.Mth.lerp(partialTick, target.yo, target.getY());
            double lerpedZ = net.minecraft.util.Mth.lerp(partialTick, target.zo, target.getZ());

            double camX = lerpedX - camPos.x;
            double camY = lerpedY - camPos.y;
            double camZ = lerpedZ - camPos.z;

            AABB box = target.getBoundingBox().move(-target.getX(), -target.getY(), -target.getZ()).move(camX, camY,
                    camZ);
            float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
            float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            switch (espMode) {
                case BOX -> {
                    // Wireframe box around entity
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.lines());
                    PoseStack.Pose pose = new PoseStack().last();
                    drawFullBox(buf, pose, x0, y0, z0, x1, y1, z1, r, g, b, 1f);
                    bufferSource.endBatch(RenderType.lines());
                }
                case FILLED -> {
                    // Semi-transparent filled box
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.debugQuads());
                    PoseStack.Pose pose = new PoseStack().last();
                    float a = 0.25f;
                    // Bottom face
                    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, a);
                    // Top face
                    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, a);
                    // Front face (z0)
                    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, a);
                    // Back face (z1)
                    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, a);
                    // Left face (x0)
                    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, a);
                    // Right face (x1)
                    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
                    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, a);
                    bufferSource.endBatch(RenderType.debugQuads());
                    // Also draw wireframe edges on top
                    VertexConsumer lineBuf = bufferSource.getBuffer(RenderType.lines());
                    PoseStack.Pose linePose = new PoseStack().last();
                    drawFullBox(lineBuf, linePose, x0, y0, z0, x1, y1, z1, r, g, b, 0.6f);
                    bufferSource.endBatch(RenderType.lines());
                }
                case CIRCLE -> {
                    // Horizontal circle at entity base
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.lines());
                    PoseStack.Pose pose = new PoseStack().last();
                    float cx = (x0 + x1) / 2f, cz = (z0 + z1) / 2f;
                    float radius = Math.max(x1 - x0, z1 - z0) * 0.6f;
                    int segments = 32;
                    for (int i = 0; i < segments; i++) {
                        double a1 = 2 * Math.PI * i / segments;
                        double a2 = 2 * Math.PI * (i + 1) / segments;
                        float px1 = cx + (float)(Math.cos(a1) * radius);
                        float pz1 = cz + (float)(Math.sin(a1) * radius);
                        float px2 = cx + (float)(Math.cos(a2) * radius);
                        float pz2 = cz + (float)(Math.sin(a2) * radius);
                        drawLine(buf, pose, px1, y0, pz1, px2, y0, pz2, r, g, b, 1f);
                    }
                    bufferSource.endBatch(RenderType.lines());
                }
                case ARROW -> {
                    // Downward arrow indicator above entity head
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.debugQuads());
                    PoseStack.Pose pose = new PoseStack().last();
                    float cx = (x0 + x1) / 2f, cz = (z0 + z1) / 2f;
                    float a = 0.6f;

                    long time = System.currentTimeMillis() % 1500;
                    float bob = (float) Math.sin(time / 1500.0 * Math.PI * 2) * 0.12f;
                    float tipY = y1 + 0.3f + bob;
                    float baseY = tipY + 0.5f;
                    float armW = 0.15f;  // half-width of arrow shaft
                    float headW = 0.3f;  // half-width of arrowhead

                    // Arrow shaft (front/back faces as quads)
                    buf.addVertex(pose, cx - armW, baseY, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx + armW, baseY, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx + armW, tipY + 0.25f, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx - armW, tipY + 0.25f, cz).setColor(r, g, b, a);

                    // Arrowhead (triangle as degenerate quad)
                    buf.addVertex(pose, cx - headW, tipY + 0.25f, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx + headW, tipY + 0.25f, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY, cz).setColor(r, g, b, a);

                    // Same on the perpendicular axis
                    buf.addVertex(pose, cx, baseY, cz - armW).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, baseY, cz + armW).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY + 0.25f, cz + armW).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY + 0.25f, cz - armW).setColor(r, g, b, a);

                    buf.addVertex(pose, cx, tipY + 0.25f, cz - headW).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY + 0.25f, cz + headW).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY, cz).setColor(r, g, b, a);
                    buf.addVertex(pose, cx, tipY, cz).setColor(r, g, b, a);

                    bufferSource.endBatch(RenderType.debugQuads());
                }
                case BEACON -> {
                    // Beam of light from entity to sky
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.debugQuads());
                    PoseStack.Pose pose = new PoseStack().last();
                    float cx = (x0 + x1) / 2f, cz = (z0 + z1) / 2f;
                    float radius = 0.15f;
                    float beamHeight = 256f;
                    float a = 0.4f;
                    int segments = 16;
                    for (int i = 0; i < segments; i++) {
                        double a1 = 2 * Math.PI * i / segments;
                        double a2 = 2 * Math.PI * (i + 1) / segments;
                        float vx1 = cx + (float)Math.cos(a1) * radius;
                        float vz1 = cz + (float)Math.sin(a1) * radius;
                        float vx2 = cx + (float)Math.cos(a2) * radius;
                        float vz2 = cz + (float)Math.sin(a2) * radius;
                        buf.addVertex(pose, vx1, y1, vz1).setColor(r, g, b, a);
                        buf.addVertex(pose, vx2, y1, vz2).setColor(r, g, b, a);
                        buf.addVertex(pose, vx2, y1 + beamHeight, vz2).setColor(r, g, b, 0.0f);
                        buf.addVertex(pose, vx1, y1 + beamHeight, vz1).setColor(r, g, b, 0.0f);
                    }
                    bufferSource.endBatch(RenderType.debugQuads());
                }
                case CYLINDER -> {
                    // Rotating cylinder around entity
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.debugQuads());
                    PoseStack.Pose pose = new PoseStack().last();
                    float cx = (x0 + x1) / 2f, cz = (z0 + z1) / 2f;
                    float radius = Math.max(x1 - x0, z1 - z0) * 0.6f;
                    float a = 0.3f;
                    int segments = 24;
                    long time = System.currentTimeMillis() % 3000;
                    double rotation = time / 3000.0 * Math.PI * 2;
                    for (int i = 0; i < segments; i++) {
                        double a1 = rotation + 2 * Math.PI * i / segments;
                        double a2 = rotation + 2 * Math.PI * (i + 1) / segments;
                        float vx1 = cx + (float)Math.cos(a1) * radius;
                        float vz1 = cz + (float)Math.sin(a1) * radius;
                        float vx2 = cx + (float)Math.cos(a2) * radius;
                        float vz2 = cz + (float)Math.sin(a2) * radius;
                        // Alternating color for stripe effect
                        float sa = (i % 2 == 0) ? a : a * 0.4f;
                        buf.addVertex(pose, vx1, y0, vz1).setColor(r, g, b, sa);
                        buf.addVertex(pose, vx2, y0, vz2).setColor(r, g, b, sa);
                        buf.addVertex(pose, vx2, y1, vz2).setColor(r, g, b, sa);
                        buf.addVertex(pose, vx1, y1, vz1).setColor(r, g, b, sa);
                    }
                    bufferSource.endBatch(RenderType.debugQuads());
                }
                case CONE -> {
                    // Cone spotlight from above — ground ring + lines to apex
                    VertexConsumer buf = bufferSource.getBuffer(RenderType.debugQuads());
                    PoseStack.Pose pose = new PoseStack().last();
                    float cx = (x0 + x1) / 2f, cz = (z0 + z1) / 2f;
                    float baseRadius = Math.max(x1 - x0, z1 - z0) * 0.7f;
                    float apexY = y1 + 2.0f;
                    float a = 0.25f;
                    int segments = 24;
                    for (int i = 0; i < segments; i++) {
                        double a1 = 2 * Math.PI * i / segments;
                        double a2 = 2 * Math.PI * (i + 1) / segments;
                        float vx1 = cx + (float)Math.cos(a1) * baseRadius;
                        float vz1 = cz + (float)Math.sin(a1) * baseRadius;
                        float vx2 = cx + (float)Math.cos(a2) * baseRadius;
                        float vz2 = cz + (float)Math.sin(a2) * baseRadius;
                        // Side panel from base to apex
                        buf.addVertex(pose, vx1, y0, vz1).setColor(r, g, b, a);
                        buf.addVertex(pose, vx2, y0, vz2).setColor(r, g, b, a);
                        buf.addVertex(pose, cx, apexY, cz).setColor(r, g, b, 0.0f);
                        buf.addVertex(pose, cx, apexY, cz).setColor(r, g, b, 0.0f);
                        // Base ring
                        buf.addVertex(pose, vx1, y0, vz1).setColor(r, g, b, a * 0.6f);
                        buf.addVertex(pose, vx2, y0, vz2).setColor(r, g, b, a * 0.6f);
                        buf.addVertex(pose, vx2, y0 + 0.02f, vz2).setColor(r, g, b, 0.0f);
                        buf.addVertex(pose, vx1, y0 + 0.02f, vz1).setColor(r, g, b, 0.0f);
                    }
                    bufferSource.endBatch(RenderType.debugQuads());
                }
                default -> {}
            }
        });
    }

    // ── Rendering helpers ──

    private static void drawFullBox(VertexConsumer buf, PoseStack.Pose pose, float x0, float y0, float z0, float x1,
            float y1, float z1, float r, float g, float b, float a) {
        drawLine(buf, pose, x0, y0, z0, x1, y0, z0, r, g, b, a);
        drawLine(buf, pose, x1, y0, z0, x1, y0, z1, r, g, b, a);
        drawLine(buf, pose, x1, y0, z1, x0, y0, z1, r, g, b, a);
        drawLine(buf, pose, x0, y0, z1, x0, y0, z0, r, g, b, a);
        drawLine(buf, pose, x0, y1, z0, x1, y1, z0, r, g, b, a);
        drawLine(buf, pose, x1, y1, z0, x1, y1, z1, r, g, b, a);
        drawLine(buf, pose, x1, y1, z1, x0, y1, z1, r, g, b, a);
        drawLine(buf, pose, x0, y1, z1, x0, y1, z0, r, g, b, a);
        drawLine(buf, pose, x0, y0, z0, x0, y1, z0, r, g, b, a);
        drawLine(buf, pose, x1, y0, z0, x1, y1, z0, r, g, b, a);
        drawLine(buf, pose, x1, y0, z1, x1, y1, z1, r, g, b, a);
        drawLine(buf, pose, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }


    private static void drawLine(VertexConsumer buf, PoseStack.Pose pose, float x0, float y0, float z0, float x1,
            float y1, float z1, float r, float g, float b, float a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0)
            return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }
}
