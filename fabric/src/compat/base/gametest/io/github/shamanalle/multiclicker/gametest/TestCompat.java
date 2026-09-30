package io.github.shamanalle.multiclicker.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Test API calls that differ between Minecraft versions. */
final class TestCompat {
    private TestCompat() {
    }

    static void waitForChunks(TestSingleplayerContext world) {
        world.getClientWorld().waitForChunksRender();
    }
}
