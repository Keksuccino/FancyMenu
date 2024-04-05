package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BridgePacketHandlerForge {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void handle(ServerPlayer sender, BridgePacketMessageForge msg, PacketHandler.PacketDirection direction) {
        if (msg.dataWithIdentifier != null) {
            PacketHandler.onPacketReceived(sender, direction, msg.dataWithIdentifier);
        }
    }

}
