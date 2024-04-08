package de.keksuccino.fancymenu.networking.packets.commands.variable.command;

import de.keksuccino.fancymenu.networking.PacketCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VariableCommandPacketCodec extends PacketCodec<VariableCommandPacket> {

    private static final Logger LOGGER = LogManager.getLogger();

    public VariableCommandPacketCodec() {
        super("variable_command", VariableCommandPacket.class);
    }

}
