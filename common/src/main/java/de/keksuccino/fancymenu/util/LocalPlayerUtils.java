package de.keksuccino.fancymenu.util;

import net.minecraft.client.player.LocalPlayer;

public class LocalPlayerUtils {

    public static void sendPlayerCommand(LocalPlayer player, String command) {
        player.chat(command);
    }

    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
        player.chat(message);
    }

}
