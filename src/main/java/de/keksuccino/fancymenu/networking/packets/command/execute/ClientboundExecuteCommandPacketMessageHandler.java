//--- 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.execute;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundExecuteCommandPacketMessageHandler implements IMessageHandler<ExecuteCommandPacketMessage, IMessage> {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ClientboundExecuteCommandPacketHandler");

    @Override
    public IMessage onMessage(ExecuteCommandPacketMessage msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ExecuteCommandPacketUtil.sendChatMessage(msg.command);
        });
        //Returning NULL because we don't want to send a response packet
        return null;
    }

}
