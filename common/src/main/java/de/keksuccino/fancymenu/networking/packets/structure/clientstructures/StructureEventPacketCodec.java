package de.keksuccino.fancymenu.networking.packets.structure.clientstructures;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class StructureEventPacketCodec extends PacketCodec<StructureEventPacket> {

    public StructureEventPacketCodec() {
        super("structure_event", StructureEventPacket.class);
    }
}
