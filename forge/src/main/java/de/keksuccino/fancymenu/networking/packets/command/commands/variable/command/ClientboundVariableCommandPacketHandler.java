package de.keksuccino.fancymenu.networking.packets.command.commands.variable.command;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundVariableCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handle(VariableCommandPacketMessage msg) {

        MainThreadTaskExecutor.executeInMainThread(() -> {
            if (msg.setOrGet != null) {
                if (msg.setOrGet.equals("set")) {

                } else {

                }
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

    }

}
