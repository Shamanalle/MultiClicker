package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Messages shown to the player only (nothing is sent to the server). */
public final class Messages {
    private Messages() {
    }

    /** A line in the chat. */
    public static void chat(LocalPlayer player, Component message) {
        player.displayClientMessage(message, false);
    }

    /** A message above the hotbar. */
    public static void overlay(LocalPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }
}
