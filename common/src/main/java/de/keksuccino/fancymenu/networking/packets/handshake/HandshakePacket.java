package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakePacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean processClientPacket(@NotNull Connection connection) {
        return ClientSideHandshakePacketLogic.handle(this, connection);
    }

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return false;
        } else {
            return ServerSideHandshakePacketLogic.handle(sender, this);
        }
    }

}
