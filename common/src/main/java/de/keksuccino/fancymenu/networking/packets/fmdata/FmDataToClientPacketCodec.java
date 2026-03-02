package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class FmDataToClientPacketCodec extends PacketCodec<FmDataToClientPacket> {

    public FmDataToClientPacketCodec() {
        super("fmdata_to_client", FmDataToClientPacket.class);
    }

}
