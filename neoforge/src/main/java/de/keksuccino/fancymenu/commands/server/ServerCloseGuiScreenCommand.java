package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.neoforge.PacketSender;
import de.keksuccino.fancymenu.networking.neoforge.packets.execute.ClientboundExecutePacketPayload;
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
                PacketSender.sendToClient(new ClientboundExecutePacketPayload("/closeguiscreen"), sender);
            } else {
                for (ServerPlayer target : targets) {
                    PacketSender.sendToClient(new ClientboundExecutePacketPayload("/closeguiscreen"), target);
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
