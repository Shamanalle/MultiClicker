package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Connection and item use calls that differ between Minecraft versions. */
public final class Session {
    private Session() {
    }

    /** The arm swing of the main hand (also seen by other players). */
    public static void swing(LocalPlayer player) {
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** Leaves the current world or server, showing the message where vanilla does. */
    public static void disconnect(Minecraft mc, Component message) {
        if (mc.isLocalServer() || mc.getConnection() == null) {
            PauseScreen.disconnectFromWorld(mc, message);
        } else {
            mc.getConnection().getConnection().disconnect(message);
        }
    }

    /**
     * Uses the item in the hand (the crosshair target is not touched) and plays the swing the way
     * vanilla does.
     *
     * @return {@code true} if the item was used
     */
    public static boolean useItem(Minecraft mc, LocalPlayer player, InteractionHand hand) {
        if (mc.gameMode.useItem(player, hand) instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                player.swing(hand);
            }
            mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
            return true;
        }
        return false;
    }
}
