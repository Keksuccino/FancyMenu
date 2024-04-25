package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class BridgePacketHandlerFabric {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final CustomPacketPayload.Type<?> TYPE = CustomPacketPayload.createType("fancymenu:packet_bridge");
    public static final ResourceLocation PACKET_ID = new ResourceLocation("fancymenu", "packet_bridge");

    public static void handle(@Nullable ServerPlayer sender, BridgePacketMessageFabric msg, PacketHandler.PacketDirection direction) {
        if (msg.dataWithIdentifier != null) {
            PacketHandler.onPacketReceived(sender, direction, msg.dataWithIdentifier);
        }
    }

    public static FriendlyByteBuf writeToByteBuf(BridgePacketMessageFabric msg) {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeUtf(msg.direction);
        buf.writeUtf(msg.dataWithIdentifier);

        return buf;

    }

    public static BridgePacketMessageFabric readFromByteBuf(FriendlyByteBuf buf) {

        BridgePacketMessageFabric msg = new BridgePacketMessageFabric();

        msg.direction = buf.readUtf();
        msg.dataWithIdentifier = buf.readUtf();

        return msg;

    }

}
