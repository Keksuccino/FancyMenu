package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class ServerNbtDataResponsePacketCodec extends PacketCodec<ServerNbtDataResponsePacket> {

    public ServerNbtDataResponsePacketCodec() {
        super("nbt_placeholder_response", ServerNbtDataResponsePacket.class);
    }

}
