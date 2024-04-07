package de.keksuccino.fancymenu.networking.packets.commands.variable.suggestions;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VariableCommandSuggestionsPacket extends Packet {

    public List<String> variable_suggestions;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) return false;
        return ServerSideVariableCommandSuggestionsPacketLogic.handle(sender, this);
    }

}