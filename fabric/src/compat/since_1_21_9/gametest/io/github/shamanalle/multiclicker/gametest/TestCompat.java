package io.github.shamanalle.multiclicker.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.world.entity.Entity;

/** Test API calls that differ between Minecraft versions. */
final class TestCompat {
    private TestCompat() {
    }

    static void waitForChunks(TestSingleplayerContext world) {
        world.getClientWorld().waitForChunksRender();
    }

    static boolean isHusk(Entity entity) {
        return entity.getType().getDescriptionId().equals("entity.minecraft.husk");
    }

    static void clearChat(Minecraft mc) {
        mc.gui.getChat().clearMessages(false);
    }

    static boolean overlayGone(Minecraft mc) {
        return mc.getOverlay() == null;
    }

    static void setHitboxes(Minecraft mc, boolean shown) {
        mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES,
                shown ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
    }

    static void setDebugScreen(Minecraft mc, boolean shown) {
        mc.debugEntries.setF3Visible(shown);
    }
}
