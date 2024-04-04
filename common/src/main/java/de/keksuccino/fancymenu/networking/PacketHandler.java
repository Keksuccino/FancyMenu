package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Consumer<String> sendToServerDataConsumer = null;
    private static BiConsumer<ServerPlayer, String> sendToClientPlayerAndDataConsumer = null;

    public static <T extends Packet> void sendToServer(@NotNull T packet) {
        Objects.requireNonNull(sendToServerDataConsumer, "Tried to send packet to server too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                sendToServerDataConsumer.accept(Objects.requireNonNull(codec.serialize(packet)));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to send packet to server!", ex);
            }
        } else {
            LOGGER.error("[FANCYMENU] No codec found for packet: " + packet.getClass(), new NullPointerException("Codec returned for packet was NULL!"));
        }
    }

    public static <T extends Packet> void sendToClient(@NotNull ServerPlayer toPlayer, @NotNull T packet) {
        Objects.requireNonNull(sendToClientPlayerAndDataConsumer, "Tried to send packet to client too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                sendToClientPlayerAndDataConsumer.accept(Objects.requireNonNull(toPlayer), Objects.requireNonNull(codec.serialize(packet)));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to send packet to client!", ex);
            }
        } else {
            LOGGER.error("[FANCYMENU] No codec found for packet: " + packet.getClass(), new NullPointerException("Codec returned for packet was NULL!"));
        }
    }

    public static void setSendToServerLogic(Consumer<String> dataConsumer) {
        sendToServerDataConsumer = dataConsumer;
    }

    public static void setSendToClientLogic(BiConsumer<ServerPlayer, String> playerAndDataConsumer) {
        sendToClientPlayerAndDataConsumer = playerAndDataConsumer;
    }

    public static void onPacketReceived(String dataWithIdentifier) {
        if (dataWithIdentifier == null) return;
        if (!dataWithIdentifier.contains(":")) return;
        String[] dataSplit = dataWithIdentifier.split(":", 2);
        PacketCodec<?> codec = PacketRegistry.getCodec(dataSplit[0]);
        if (codec == null) {
            LOGGER.error("[FANCYMENU] No codec for packet data found with identifier: " + dataSplit[0], new NullPointerException("Codec returned for identifier was NULL!"));
            return;
        }
        if (Services.PLATFORM.isOnClient()) {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                try {
                    Packet packet = Objects.requireNonNull(codec.deserialize(dataSplit[1]));
                    packet.processPacket();
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to process packet on client!", ex);
                }
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        } else {
            try {
                Packet packet = Objects.requireNonNull(codec.deserialize(dataSplit[1]));
                packet.processPacket();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to process packet on server!", ex);
            }
        }
    }

}
