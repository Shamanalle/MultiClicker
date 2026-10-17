package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.mixin.DisconnectedScreenAccessor;

/** Reading why the connection was lost and joining a server again, which differ between versions. */
public final class Reconnect {
    private Reconnect() {
    }

    /** The reason shown on the screen after the connection was lost. */
    public static Component reason(DisconnectedScreen screen) {
        return ((DisconnectedScreenAccessor) screen).multiclicker$reason();
    }

    /** Joins the server the way the server list does. */
    public static void connect(Minecraft mc, Screen parent, ServerData server) {
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(server.ip), server, false);
    }
}
