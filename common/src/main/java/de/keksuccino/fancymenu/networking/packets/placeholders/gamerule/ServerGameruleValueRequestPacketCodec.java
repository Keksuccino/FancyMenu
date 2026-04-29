package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class ServerGameruleValueRequestPacketCodec extends PacketCodec<ServerGameruleValueRequestPacket> {

    public ServerGameruleValueRequestPacketCodec() {
        super("gamerule_placeholder_request", ServerGameruleValueRequestPacket.class);
    }

}
