package de.keksuccino.fancymenu.networking.packets.command.commands.variable.suggestions;

import de.keksuccino.fancymenu.networking.PacketMessageBaseForge;
import java.util.ArrayList;
import java.util.List;

public class VariableCommandSuggestionsPacketMessage extends PacketMessageBaseForge {

    public List<String> variableNameSuggestions = new ArrayList<>();

}
