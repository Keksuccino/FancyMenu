//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class ServerCloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(CommandSourceStack stack) {
        try {
            //Send packet to client, so it knows it needs to execute the real client-side /closeguiscreen command
            ServerPlayer sender = stack.getPlayerOrException();
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.direction = "client";
            msg.command = "/closeguiscreen";
            ServerPlayNetworking.send(sender, ClientboundExecuteCommandPacketHandler.PACKET_ID, ClientboundExecuteCommandPacketHandler.writeToByteBuf(msg));
        } catch (Exception e) {
            stack.sendFailure(new TextComponent("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
