package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class ServerCloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource(), null);
        }).then(Commands.argument("target_players", EntityArgument.players())
                .requires(stack -> stack.hasPermission(2))
                .executes(stack -> {
                    return closeGui(stack.getSource(), EntityArgument.getPlayers(stack, "target_players"));
                })));
    }

    private static int closeGui(CommandSourceStack stack, @Nullable Collection<ServerPlayer> targets) {
        try {
            if (targets == null) {
                //Send packet to client, so it knows it needs to execute the real client-side /closeguiscreen command
                ServerPlayer sender = stack.getPlayerOrException();
                ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                msg.direction = "client";
                msg.command = "/closeguiscreen";
                ServerPlayNetworking.send(sender, ClientboundExecuteCommandPacketHandler.PACKET_ID, ClientboundExecuteCommandPacketHandler.writeToByteBuf(msg));
            } else {
                for (ServerPlayer target : targets) {
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.direction = "client";
                    msg.command = "/closeguiscreen";
                    ServerPlayNetworking.send(target, ClientboundExecuteCommandPacketHandler.PACKET_ID, ClientboundExecuteCommandPacketHandler.writeToByteBuf(msg));
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Components.literal("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
