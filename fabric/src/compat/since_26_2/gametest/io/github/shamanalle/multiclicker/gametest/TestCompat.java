package io.github.shamanalle.multiclicker.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Test API calls that differ between Minecraft versions. */
final class TestCompat {
    private TestCompat() {
    }

    static void waitForChunks(TestSingleplayerContext world) {
        world.getConnection().waitForChunksRender();
    }

    static boolean isHusk(Entity entity) {
        return entity.getType().getDescriptionId().equals("entity.minecraft.husk");
    }

    static void clearChat(Minecraft mc) {
        mc.gui.hud.getChat().clearMessages(false);
    }

    static boolean overlayGone(Minecraft mc) {
        return mc.gui.overlay() == null;
    }
}
