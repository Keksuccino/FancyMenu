package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.packets.command.execute.ClientboundExecuteCommandPacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.konkrete.command.CommandUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class ServerOpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"), null);
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
                .then(Commands.argument("target_players", EntityArgument.players())
                        .requires(stack -> stack.hasPermission(2))
                        .executes(stack -> {
                            return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"), EntityArgument.getPlayers(stack, "target_players"));
                        }))
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName, @Nullable Collection<ServerPlayer> targets) {
        try {
            if (targets == null) {
                ServerPlayer sender = stack.getPlayerOrException();
                ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                msg.direction = "client";
                msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
                ServerPlayNetworking.send(sender, ClientboundExecuteCommandPacketHandler.PACKET_ID, ClientboundExecuteCommandPacketHandler.writeToByteBuf(msg));
            } else {
                for (ServerPlayer target : targets) {
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.direction = "client";
                    msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
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