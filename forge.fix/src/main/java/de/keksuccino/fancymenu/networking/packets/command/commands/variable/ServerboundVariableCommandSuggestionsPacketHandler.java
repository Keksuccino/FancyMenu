
package de.keksuccino.fancymenu.networking.packets.command.commands.variable;

import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import net.minecraft.server.level.ServerPlayer;

public class ServerboundVariableCommandSuggestionsPacketHandler {

    public static void handle(VariableCommandSuggestionsPacketMessage msg, ServerPlayer sender) {

        if (sender != null) {
            String uuid = sender.getUUID().toString();
            ServerVariableCommand.cachedVariableArguments.put(uuid, msg.variableNameSuggestions);
        }

    }

}
