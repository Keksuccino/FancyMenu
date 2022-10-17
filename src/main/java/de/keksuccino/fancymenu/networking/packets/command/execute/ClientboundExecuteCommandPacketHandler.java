//TODO übernehmen 2.12.1
package de.keksuccino.fancymenu.networking.packets.command.execute;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientboundExecuteCommandPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ClientboundExecuteCommandPacketHandler");
    public static final ResourceLocation PACKET_ID = new ResourceLocation("fancymenu", "execute_command");

    public static void handle(ExecuteCommandPacketMessage msg) {

        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().player.chat(msg.command);
        });

    }

    public static FriendlyByteBuf writeToByteBuf(ExecuteCommandPacketMessage msg) {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeUtf(msg.direction);
        buf.writeUtf(msg.command);

        return buf;

    }

    public static ExecuteCommandPacketMessage readFromByteBuf(FriendlyByteBuf buf) {

        ExecuteCommandPacketMessage msg = new ExecuteCommandPacketMessage();

        msg.direction = buf.readUtf();
        msg.command = buf.readUtf();

        return msg;

    }

}
