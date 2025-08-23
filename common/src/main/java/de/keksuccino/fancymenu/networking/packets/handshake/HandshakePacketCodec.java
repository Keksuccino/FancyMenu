package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class HandshakePacketCodec extends PacketCodec<HandshakePacket> {

    public HandshakePacketCodec() {
        super("fancymenu_handshake", HandshakePacket.class);
    }

}
