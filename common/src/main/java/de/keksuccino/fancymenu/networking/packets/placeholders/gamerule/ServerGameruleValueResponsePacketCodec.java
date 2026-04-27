package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class ServerGameruleValueResponsePacketCodec extends PacketCodec<ServerGameruleValueResponsePacket> {

    public ServerGameruleValueResponsePacketCodec() {
        super("gamerule_placeholder_response", ServerGameruleValueResponsePacket.class);
    }

}
