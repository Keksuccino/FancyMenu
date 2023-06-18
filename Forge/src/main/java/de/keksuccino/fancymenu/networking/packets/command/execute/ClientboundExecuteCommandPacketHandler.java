
package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.LocalPlayerUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundExecuteCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handle(ExecuteCommandPacketMessage msg) {

        MainThreadTaskExecutor.executeInMainThread(() -> {
            //TODO Only 1.19+
            if ((msg.command != null) && msg.command.startsWith("/")) {
                msg.command = msg.command.substring(1);
            }
            LocalPlayerUtils.sendPlayerCommand(Minecraft.getInstance().player, msg.command);
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

    }

}
