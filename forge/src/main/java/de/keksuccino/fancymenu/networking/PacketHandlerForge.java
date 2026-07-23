package de.keksuccino.fancymenu.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandlerForge {

    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation("fancymenu", "play"), () -> PROTOCOL_VERSION, s -> true, s -> true);

    private static int messageIndex = -1;

    public static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        messageIndex++;
        INSTANCE.registerMessage(messageIndex, messageType, encoder, decoder, messageConsumer);
    }

    public static boolean isRemotePresent(@NotNull Connection connection) {
        return INSTANCE.isRemotePresent(Objects.requireNonNull(connection));
    }

    public static boolean sendToServer(@NotNull Connection connection, @NotNull Object message) {
        Connection exactConnection = Objects.requireNonNull(connection);
        if (!INSTANCE.isRemotePresent(exactConnection)) return false;
        INSTANCE.sendTo(Objects.requireNonNull(message), exactConnection, NetworkDirection.PLAY_TO_SERVER);
        return true;
    }

    public static boolean sendToClient(@NotNull ServerPlayer player, @NotNull Object message) {
        Connection exactConnection = Objects.requireNonNull(Objects.requireNonNull(player).connection.connection);
        if (!INSTANCE.isRemotePresent(exactConnection)) return false;
        INSTANCE.sendTo(Objects.requireNonNull(message), exactConnection, NetworkDirection.PLAY_TO_CLIENT);
        return true;
    }

}
