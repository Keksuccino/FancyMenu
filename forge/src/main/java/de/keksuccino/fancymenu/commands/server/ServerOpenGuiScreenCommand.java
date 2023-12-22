package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.commands.client.CommandUtils;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
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
                ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                msg.direction = "client";
                msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
                PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
            } else {
                for (ServerPlayer target : targets) {
                    ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
                    msg.direction = "client";
                    msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
                    PacketHandler.send(PacketDistributor.PLAYER.with(() -> target), msg);
                }
            }
        } catch (Exception e) {
            stack.sendFailure(Components.literal("Error while trying to execute command!"));
            e.printStackTrace();
        }
        return 1;
    }

}