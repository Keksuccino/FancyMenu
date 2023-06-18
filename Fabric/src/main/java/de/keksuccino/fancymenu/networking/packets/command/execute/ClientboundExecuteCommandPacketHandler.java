
package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.util.LocalPlayerUtils;
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
            //Only 1.19+
            if ((msg.command != null) && msg.command.startsWith("/")) {
                msg.command = msg.command.substring(1);
            }
            LocalPlayerUtils.sendPlayerCommand(Minecraft.getInstance().player, msg.command);
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
