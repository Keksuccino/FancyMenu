package de.keksuccino.fancymenu.networking.packets.command.commands.opengui;

import de.keksuccino.fancymenu.commands.opengui.OpenGuiCommandLogic;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundOpenGuiCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handle(OpenGuiCommandPacketMessage msg) {

        MainThreadTaskExecutor.executeInMainThread(() -> {
            if (msg.screenIdentifier != null) {
                OpenGuiCommandLogic.openGui(msg.screenIdentifier);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

    }

}
