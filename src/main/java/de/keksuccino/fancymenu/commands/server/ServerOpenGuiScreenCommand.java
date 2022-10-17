//---
package de.keksuccino.fancymenu.commands.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.commands.client.CommandUtils;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.command.execute.ExecuteCommandPacketMessage;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class ServerOpenGuiScreenCommand {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("openguiscreen").then(Commands.argument("menu_identifier", StringArgumentType.string())
                .executes((stack) -> {
                    return openGui(stack.getSource(), StringArgumentType.getString(stack, "menu_identifier"));
                })
                .suggests((context, provider) -> {
                    return CommandUtils.getStringSuggestions(provider, "<menu_identifier>");
                })
        ));
    }

    private static int openGui(CommandSource stack, String menuIdentifierOrCustomGuiName) {
        try {
            //Send packet to sender, so the client can execute the real client-side command
            ServerPlayerEntity sender = stack.getPlayerOrException();
            ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();
            msg.direction = "client";
            msg.command = "/openguiscreen " + menuIdentifierOrCustomGuiName;
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> sender), msg);
        } catch (Exception e) {
            stack.sendFailure(new StringTextComponent("Error while trying to execute command!"));
            e.printStackTrace();
        }
        return 1;
    }

}