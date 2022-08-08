//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.commands.client.ClientExecutor;
import de.keksuccino.fancymenu.compatibility.MinecraftCompatibilityUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundExecuteCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ClientboundExecuteCommandPacketHandler");

    public static void handle(ExecuteCommandPacketMessage msg) {

        ClientExecutor.execute(() -> {
            //Only 1.19+
            if ((msg.command != null) && msg.command.startsWith("/")) {
                msg.command = msg.command.substring(1);
            }
            MinecraftCompatibilityUtils.sendPlayerCommand(Minecraft.getInstance().player, msg.command);
        });

    }

}
