package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ExecuteCommandPacketUtil {

    public static void sendChatMessage(String msg) {
        try {
            GuiScreen s = new CustomGuiBase("", "", true, null, null);
            s.setWorldAndResolution(Minecraft.getMinecraft(), 1000, 1000);
            s.sendChatMessage(msg, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
