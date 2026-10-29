package de.keksuccino.fancymenu.networking.packets.raid;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class ActiveRaidPacketCodec extends PacketCodec<ActiveRaidPacket> {

    public ActiveRaidPacketCodec() {
        super("active_raid", ActiveRaidPacket.class);
    }

}
