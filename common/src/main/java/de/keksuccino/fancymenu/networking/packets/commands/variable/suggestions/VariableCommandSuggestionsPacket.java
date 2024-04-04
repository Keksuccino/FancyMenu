package de.keksuccino.fancymenu.networking.packets.commands.variable.suggestions;

import de.keksuccino.fancymenu.networking.Packet;
import java.util.List;

public class VariableCommandSuggestionsPacket extends Packet {

    public List<String> variable_suggestions;

    @Override
    public boolean processPacket() {
        return false;
    }

}