package io.github.shamanalle.multiclicker.fabric;

/**
 * Fabric API for Minecraft 1.21.9 has no world rendering events, so the outline and filled box
 * styles of the target highlight are not drawn there (the glow style still works).
 */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
    }
}
