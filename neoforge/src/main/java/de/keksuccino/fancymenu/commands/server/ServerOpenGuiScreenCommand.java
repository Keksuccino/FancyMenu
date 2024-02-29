package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.commands.client.CommandUtils;
import de.keksuccino.fancymenu.networking.neoforge.PacketSender;
import de.keksuccino.fancymenu.networking.neoforge.packets.execute.ClientboundExecutePacketPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class ServerOpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("screen_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "screen_identifier"), null);
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<screen_identifier>");
                })
                .then(Commands.argument("target_players", EntityArgument.players())
                        .requires(stack -> stack.hasPermission(2))
                        .executes(stack -> {
                            return openGui(stack.getSource(), StringArgumentType.getString(stack, "screen_identifier"), EntityArgument.getPlayers(stack, "target_players"));
                        }))
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName, @Nullable Collection<ServerPlayer> targets) {
        try {
            if (targets == null) {
                //Send packet to sender, so the client can execute the real client-side command
                ServerPlayer sender = stack.getPlayerOrException();
                PacketSender.sendToClient(new ClientboundExecutePacketPayload("/openguiscreen " + menuIdentifierOrCustomGuiName), sender);
            } else {
                for (ServerPlayer target : targets) {
                    PacketSender.sendToClient(new ClientboundExecutePacketPayload("/openguiscreen " + menuIdentifierOrCustomGuiName), target);
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Error while trying to execute command!"));
            e.printStackTrace();
        }
        return 1;
    }

}