//--- 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.commands.client.ClientExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundExecuteCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ClientboundExecuteCommandPacketHandler");

    public static void handle(ExecuteCommandPacketMessage msg) {

        ClientExecutor.execute(() -> {
            Screen s = new ChatScreen("");
            s.init(Minecraft.getInstance(), 1000, 1000);
            s.sendMessage(msg.command, true);
        });

    }

}
