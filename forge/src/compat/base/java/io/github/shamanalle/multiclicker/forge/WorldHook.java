package io.github.shamanalle.multiclicker.forge;

/**
 * Forge for Minecraft 1.21.2 and newer has no world rendering event (only frame passes), so the outline and filled highlight
 * styles are not drawn; the glow style works.
 */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
    }
}
