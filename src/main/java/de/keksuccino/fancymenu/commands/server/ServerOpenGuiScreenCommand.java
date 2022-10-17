//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import de.keksuccino.konkrete.command.CommandUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ServerOpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName) {
        try {
            ServerPlayer sender = stack.getPlayerOrException();
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.direction = "client";
            msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
            ServerPlayNetworking.send(sender, ClientboundExecuteCommandPacketHandler.PACKET_ID, ClientboundExecuteCommandPacketHandler.writeToByteBuf(msg));
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}