package de.keksuccino.fancymenu.networking.packets.commands.closegui;

import de.keksuccino.fancymenu.networking.PacketCodec;

public class CloseGuiCommandPacketCodec extends PacketCodec<CloseGuiCommandPacket> {

    public CloseGuiCommandPacketCodec() {
        super("close_gui_command", CloseGuiCommandPacket.class);
    }

}
