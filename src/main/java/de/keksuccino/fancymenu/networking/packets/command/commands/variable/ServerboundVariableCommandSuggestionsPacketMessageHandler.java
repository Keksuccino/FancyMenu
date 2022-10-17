package de.keksuccino.fancymenu.networking.packets.command.commands.variable;

import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ServerboundVariableCommandSuggestionsPacketMessageHandler implements IMessageHandler<VariableCommandSuggestionsPacketMessage, IMessage> {

    @Override
    public IMessage onMessage(VariableCommandSuggestionsPacketMessage msg, MessageContext ctx) {
        EntityPlayerMP sender = ctx.getServerHandler().player;
        if (sender != null) {
            sender.getServerWorld().addScheduledTask(() -> {
                String uuid = sender.getUniqueID().toString();
                ServerVariableCommand.cachedVariableArguments.put(uuid, msg.variableNameSuggestions);
            });
        }
        //Returning NULL because we don't want to send a response packet
        return null;
    }

}
