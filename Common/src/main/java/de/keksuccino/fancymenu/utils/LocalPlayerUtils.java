package de.keksuccino.fancymenu.utils;

import net.minecraft.client.player.LocalPlayer;

public class LocalPlayerUtils {

    public static void sendPlayerCommand(LocalPlayer player, String command) {
        player.connection.sendCommand(command);
    }

    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
        player.connection.sendChat(message);
    }

}
