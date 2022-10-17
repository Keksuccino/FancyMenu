//---
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class ServerCloseGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("closeguiscreen").executes((stack) -> {
            return closeGui(stack.getSource());
        }));
    }

    private static int closeGui(CommandSource stack) {
        try {
            //Send packet to client, so it knows it needs to execute the real client-side /closeguiscreen command
            ServerPlayerEntity sender = stack.getPlayerOrException();
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.direction = "client";
            msg.command = "/closeguiscreen";
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
        } catch (Exception e) {
            stack.sendFailure(new StringTextComponent("Error while executing command!"));
            e.printStackTrace();
        }
        return 1;
    }

}
