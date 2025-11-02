package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class ServerNbtDataRequestPacketCodec extends PacketCodec<ServerNbtDataRequestPacket> {

    public ServerNbtDataRequestPacketCodec() {
        super("nbt_placeholder_request", ServerNbtDataRequestPacket.class);
    }

}
