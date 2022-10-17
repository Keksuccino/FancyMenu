//---
package de.keksuccino.fancymenu.networking;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {

    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("fancymenu", "play"), () -> PROTOCOL_VERSION, s -> true, s -> true);

    private static int messageIndex = -1;

    public static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        messageIndex++;
        INSTANCE.registerMessage(messageIndex, messageType, encoder, decoder, messageConsumer);
    }

    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        INSTANCE.send(target, message);
    }

}
