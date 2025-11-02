package de.keksuccino.fancymenu.networking.packets.entities;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class EntityEventPacketCodec extends PacketCodec<EntityEventPacket> {

    public EntityEventPacketCodec() {
        super("entity_event", EntityEventPacket.class);
    }
}
