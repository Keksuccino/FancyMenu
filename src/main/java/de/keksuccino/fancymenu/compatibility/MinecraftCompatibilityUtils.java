
package de.keksuccino.fancymenu.compatibility;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MinecraftCompatibilityUtils {

    private static final String VERSION = FancyMenu.getMinecraftVersion();

    public static void sendPlayerCommand(LocalPlayer player, String command) {
        //MC 1.19
        if (VERSION.equals("1.19")) {
            try {
                Method m = ReflectionHelper.findMethod(LocalPlayer.class, "command", "method_44099", String.class); //command
                m.invoke(player, command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //MC 1.19.1
        else {
            try {
                Method m = ReflectionHelper.findMethod(LocalPlayer.class, "commandSigned", "method_44098", String.class, Component.class); //commandSigned
                m.invoke(player, command, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
        //MC 1.19
        if (VERSION.equals("1.19")) {
            try {
                Method m = ReflectionHelper.findMethod(LocalPlayer.class, "chat", "method_3142", String.class); //chat
                m.invoke(player, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //MC 1.19.1
        else {
            try {
                Method m = ReflectionHelper.findMethod(LocalPlayer.class, "chatSigned", "method_44096", String.class, Component.class); //chatSigned
                m.invoke(player, message, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Screen getTitleScreenRealmsNotificationsScreen(TitleScreen titleScreen) {
        try {
            Field f = ReflectionHelper.findField(TitleScreen.class, "realmsNotificationsScreen", "field_2592");
            return (Screen) f.get(titleScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
