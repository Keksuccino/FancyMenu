package de.keksuccino.fancymenu.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketHandler {

    public static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(new ResourceLocation("fancymenu", "play")).networkProtocolVersion(PROTOCOL_VERSION).acceptedVersions((status, version) -> true).simpleChannel();

    public static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, CustomPayloadEvent.Context> messageConsumer) {
        INSTANCE.messageBuilder(messageType).encoder(encoder).decoder(decoder).consumerMainThread(messageConsumer).add();
    }

    public static <MSG> void registerMessage(Class<MSG> messageType, NetworkDirection direction, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, CustomPayloadEvent.Context> messageConsumer) {
        INSTANCE.messageBuilder(messageType, direction).encoder(encoder).decoder(decoder).consumerMainThread(messageConsumer).add();
    }

    public static void sendToServer(Object message) {
        if (Minecraft.getInstance().getConnection() == null) return;
        INSTANCE.send(message, Minecraft.getInstance().getConnection().getConnection());
    }

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        INSTANCE.send(message, target);
    }

}
