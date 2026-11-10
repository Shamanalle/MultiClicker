package io.github.shamanalle.multiclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;

/** Distinguishes real connected players from server NPCs and anti-cheat bot entities. */
public final class Players {
    private Players() {
    }

    /**
     * Whether {@code candidate} is another real player on the server rather than the local player,
     * a spectator, a Citizens/ZNPCs statue (UUID version 2 or removed from the tab list), or a
     * nametag-spoofed bot entity.
     */
    public static boolean isRealPlayer(Minecraft mc, Player candidate) {
        if (candidate == null || candidate == mc.player || candidate.isSpectator()) {
            return false;
        }
        // Citizens and ZNPCs mark server-side NPCs with version-2 UUIDs even before removing them from Tab.
        if (candidate.getUUID().version() == 2) {
            return false;
        }
        String name = candidate.getName().getString();
        if (name.isEmpty() || name.indexOf(' ') >= 0 || name.indexOf('§') >= 0) {
            return false;
        }
        ClientPacketListener connection = mc.getConnection();
        return connection == null || connection.getPlayerInfo(candidate.getUUID()) != null;
    }
}
