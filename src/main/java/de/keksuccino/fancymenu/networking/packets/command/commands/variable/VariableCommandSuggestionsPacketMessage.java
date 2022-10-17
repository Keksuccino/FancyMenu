//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.commands.variable;

import de.keksuccino.fancymenu.networking.PacketMessageBase;

import java.util.ArrayList;
import java.util.List;

public class VariableCommandSuggestionsPacketMessage extends PacketMessageBase {

    public List<String> variableNameSuggestions = new ArrayList<>();

}
