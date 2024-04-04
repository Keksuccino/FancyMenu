package de.keksuccino.fancymenu.networking.packets.command.commands.variable.command;

import de.keksuccino.fancymenu.networking.PacketMessageBaseForge;

public class VariableCommandPacketMessage extends PacketMessageBaseForge {

    public String setOrGet;
    public String variableName;
    public String setToValue;
    public boolean feedback;

}
