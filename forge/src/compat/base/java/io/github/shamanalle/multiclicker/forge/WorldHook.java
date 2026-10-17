package io.github.shamanalle.multiclicker.forge;

/**
 * Forge for Minecraft 1.21.2 and newer has no world rendering event (only frame passes); the target highlight is
 * drawn by LevelRendererMixin instead.
 */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
    }
}
