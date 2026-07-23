package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideHandshakePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull HandshakePacket packet, @NotNull Connection connection) {
        if (PacketHandler.addFancyMenuServer(connection)) LOGGER.info("[FANCYMENU] Connected to a server with FancyMenu installed: " + connection.getRemoteAddress());
        return true;
    }

}
