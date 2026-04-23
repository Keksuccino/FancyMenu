package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class FmDataToServerPacketCodec extends PacketCodec<FmDataToServerPacket> {

    public FmDataToServerPacketCodec() {
        super("fmdata_to_server", FmDataToServerPacket.class);
    }

}
