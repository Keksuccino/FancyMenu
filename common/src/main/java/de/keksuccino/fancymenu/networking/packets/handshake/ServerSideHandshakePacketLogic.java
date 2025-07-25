package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServerSideHandshakePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull HandshakePacket packet) {
        PacketHandler.addFancyMenuClient(sender.getUUID().toString());
        LOGGER.info("[FANCYMENU] A client with FancyMenu installed joined the server: " + sender.getScoreboardName());
        return true;
    }

}
