//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.command.commands.variable.ServerboundVariableCommandSuggestionsPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.commands.variable.VariableCommandSuggestionsPacketMessage;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class Packets {

    public static void registerAll() {

        //VARIABLE COMMAND SUGGESTIONS || SERVER-BOUND
        ServerPlayNetworking.registerGlobalReceiver(ServerboundVariableCommandSuggestionsPacketHandler.PACKET_ID, (server, player, handler, buf, responseSender) -> {
            VariableCommandSuggestionsPacketMessage msg = ServerboundVariableCommandSuggestionsPacketHandler.readFromByteBuf(buf);
            ServerboundVariableCommandSuggestionsPacketHandler.handle(msg, player);
        });

        //EXECUTE COMMAND || CLIENT-BOUND
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(ClientboundExecuteCommandPacketHandler.PACKET_ID, (client, handler, buf, responseSender) -> {
                ExecuteCommandPacketMessage msg = ClientboundExecuteCommandPacketHandler.readFromByteBuf(buf);
                ClientboundExecuteCommandPacketHandler.handle(msg);
            });
        }

    }

}
