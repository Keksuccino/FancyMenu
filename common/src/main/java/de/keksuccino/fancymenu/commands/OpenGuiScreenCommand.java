package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.commands.opengui.OpenGuiCommandPacket;
import de.keksuccino.fancymenu.util.CommandUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class OpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("screen_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "screen_identifier"), null);
                })
                .suggests((context, provider) -> {
                    return CommandUtils.buildStringSuggestionsList(context, "<screen_identifier>");
                })
                .then(Commands.argument("target_players", EntityArgument.players())
                        .requires(stack -> stack.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .executes(stack -> {
                            return openGui(stack.getSource(), StringArgumentType.getString(stack, "screen_identifier"), EntityArgument.getPlayers(stack, "target_players"));
                        }))
        ));
    }

    private static int openGui(CommandSourceStack stack, String menuIdentifierOrCustomGuiName, @Nullable Collection<ServerPlayer> targets) {
        try {
            if (targets == null) {
                ServerPlayer sender = stack.getPlayerOrException();
                OpenGuiCommandPacket packet = new OpenGuiCommandPacket();
                packet.screen_identifier = menuIdentifierOrCustomGuiName;
                PacketHandler.sendToClient(sender, packet);
            } else {
                for (ServerPlayer target : targets) {
                    OpenGuiCommandPacket packet = new OpenGuiCommandPacket();
                    packet.screen_identifier = menuIdentifierOrCustomGuiName;
                    PacketHandler.sendToClient(target, packet);
                }
            }
        } catch (Exception ex) {
            stack.sendFailure(Component.literal("Error while executing command!"));
            ex.printStackTrace();
        }
        return 1;
    }

}