
package de.keksuccino.fancymenu.compatibility;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.player.LocalPlayer;

public class MinecraftCompatibilityUtils {

    private static final String VERSION = FancyMenu.getMinecraftVersion();

    public static void sendPlayerCommand(LocalPlayer player, String command) {
//        //MC 1.19
//        if (VERSION.equals("1.19")) {
//            try {
//                Method m = ObfuscationReflectionHelper.findMethod(LocalPlayer.class, "m_234156_", String.class); //command
//                m.invoke(player, command);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        //MC 1.19.1
//        else {
//            try {
//                Method m = ObfuscationReflectionHelper.findMethod(LocalPlayer.class, "m_234148_", String.class, Component.class); //commandSigned
//                m.invoke(player, command, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        player.connection.sendCommand(command);
    }

    public static void sendPlayerChatMessage(LocalPlayer player, String message) {
//        //MC 1.19
//        if (VERSION.equals("1.19")) {
//            try {
//                Method m = ObfuscationReflectionHelper.findMethod(LocalPlayer.class, "m_108739_", String.class); //chat
//                m.invoke(player, message);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        //MC 1.19.1
//        else {
//            try {
//                Method m = ObfuscationReflectionHelper.findMethod(LocalPlayer.class, "m_240287_", String.class, Component.class); //chatSigned
//                m.invoke(player, message, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        player.connection.sendChat(message);
    }

}
