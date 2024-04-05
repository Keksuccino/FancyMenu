package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.commands.closegui.CloseGuiCommandPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public class CloseGuiScreenCommand {

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
                ServerPlayer sender = stack.getPlayerOrException();
                CloseGuiCommandPacket packet = new CloseGuiCommandPacket();
                PacketHandler.sendToClient(sender, packet);
            } else {
                for (ServerPlayer target : targets) {
                    CloseGuiCommandPacket packet = new CloseGuiCommandPacket();
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
