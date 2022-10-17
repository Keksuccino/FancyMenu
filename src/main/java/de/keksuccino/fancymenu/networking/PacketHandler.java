//--- 2.12.1
package de.keksuccino.fancymenu.networking;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("fancymenu_play");

    private static int messageIndex = -1;

    public static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> requestMessageType, Side side) {
        messageIndex++;
        INSTANCE.registerMessage(messageHandler, requestMessageType, messageIndex, side);
    }

    public static void sendToServer(IMessage message) {
        INSTANCE.sendToServer(message);
    }

    public static void sendTo(EntityPlayerMP target, IMessage message) {
        INSTANCE.sendTo(message, target);
    }

    public static void sendToAll(IMessage message) {
        INSTANCE.sendToAll(message);
    }

}
