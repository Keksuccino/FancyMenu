//--- 2.12.1
package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.command.commands.variable.ServerboundVariableCommandSuggestionsPacketMessageHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketMessageHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraftforge.fml.relauncher.Side;

public class Packets {

    public static void registerAll() {

        //EXECUTE COMMAND
        PacketHandler.registerMessage(ClientboundExecuteCommandPacketMessageHandler.class, ExecuteCommandPacketMessage.class, Side.CLIENT);

        //VARIABLE COMMAND SUGGESTIONS HANDLING
        PacketHandler.registerMessage(ServerboundVariableCommandSuggestionsPacketMessageHandler.class, VariableCommandSuggestionsPacketMessage.class, Side.SERVER);
        PacketHandler.registerMessage(ServerboundVariableCommandSuggestionsPacketMessageHandler.class, VariableCommandSuggestionsPacketMessage.class, Side.CLIENT);

    }

}
