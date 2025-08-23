package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.networking.ClientPacketUtils;
import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideHandshakePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull HandshakePacket packet) {
        if (Minecraft.getInstance().getConnection() == null) {
            LOGGER.error("[FANCYMENU] Failed to handle handshake packet! Connection was NULL!", new NullPointerException("Connection was NULL!"));
            return false;
        }
        String ip = ClientPacketUtils.getConnectedServerIp();
        if (ip == null) {
            LOGGER.error("[FANCYMENU] Failed to handle handshake packet! IP was NULL!", new NullPointerException("IP was NULL!"));
            return false;
        }
        PacketHandler.addFancyMenuServer(ip);
        LOGGER.info("[FANCYMENU] Connected to a server with FancyMenu installed: " + ip);
        return true;
    }

}
