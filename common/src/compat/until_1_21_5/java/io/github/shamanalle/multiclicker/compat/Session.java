package io.github.shamanalle.multiclicker.compat;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Connection and item use calls that differ between Minecraft versions. */
public final class Session {
    private Session() {
    }

    /** Leaves the current world or server, like the "Save and Quit" / "Disconnect" button. */
    public static void disconnect(Minecraft mc, Component message) {
        if (mc.getConnection() == null || mc.level == null) {
            return;
        }
        if (!mc.isLocalServer()) {
            mc.getConnection().getConnection().disconnect(message);
            return;
        }
        ServerData server = mc.getCurrentServer();
        mc.level.disconnect();
        mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        TitleScreen title = new TitleScreen();
        if (server != null && server.isRealm()) {
            mc.setScreen(new RealmsMainScreen(title));
        } else {
            mc.setScreen(title);
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
