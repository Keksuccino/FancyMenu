package de.keksuccino.fancymenu.compatibility;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.client.IMixinTitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;

public class MinecraftCompatibilityUtils {

    private static final String VERSION = FancyMenu.getMinecraftVersion();

    public static void sendPlayerCommand(LocalPlayer player, String command) {
        player.connection.sendCommand(command);
    }

    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
        player.connection.sendChat(message);
    }

    public static Screen getTitleScreenRealmsNotificationsScreen(TitleScreen titleScreen) {
        return ((IMixinTitleScreen)titleScreen).getRealmsNotificationsScreenFancyMenu();
    }

}
