//--- 2.12.1
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

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
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
        } catch (Exception e) {
            stack.sendFailure(new TextComponent("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
