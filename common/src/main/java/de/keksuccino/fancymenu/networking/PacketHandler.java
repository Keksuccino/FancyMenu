package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.handshake.HandshakePacket;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    static final List<String> FANCYMENU_SERVERS = new ArrayList<>();
    private static final List<String> FANCYMENU_CLIENTS = new ArrayList<>();
    private static final ServerHandshakeNegotiationTracker SERVER_HANDSHAKE_NEGOTIATIONS = new ServerHandshakeNegotiationTracker();

    private static Consumer<String> sendToServerDataConsumer = null;
    private static BiConsumer<ServerPlayer, String> sendToClientPlayerAndDataConsumer = null;

    public static void addFancyMenuServer(@NotNull String serverIp) {
        Objects.requireNonNull(serverIp);
        if (FANCYMENU_SERVERS.contains(serverIp)) return;
        FANCYMENU_SERVERS.add(serverIp);
    }

    /**
     * @return true only for the first accepted handshake on this live play connection
     */
    public static boolean addFancyMenuClient(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.accept(Objects.requireNonNull(player.connection));
        warnAboutRejectedHandshake(player, decision);
        if (!decision.isAllowed()) return false;

        addFancyMenuClient(player.getUUID().toString());
        return true;
    }

    public static void addFancyMenuClient(@NotNull String playerUUID) {
        Objects.requireNonNull(playerUUID);
        if (!FANCYMENU_CLIENTS.contains(playerUUID)) FANCYMENU_CLIENTS.add(playerUUID);
    }

    public static boolean isFancyMenuClient(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        return FANCYMENU_CLIENTS.contains(player.getUUID().toString());
    }

    public static void sendHandshakeToClient(@NotNull ServerPlayer player) {
        sendToClient(player, new HandshakePacket());
    }

    public static void sendHandshakeToServer() {
        sendToServer(new HandshakePacket());
    }

    public static <T extends Packet> void sendToServer(@NotNull T packet) {
        if (!ClientPacketUtils.shouldSendToServer(packet)) return;
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
        if (!(packet instanceof HandshakePacket) && !FANCYMENU_CLIENTS.contains(toPlayer.getUUID().toString())) return;
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

    public static <T extends Packet> void sendToAllFancyMenuClients(@NotNull MinecraftServer server, @NotNull T packet) {
        Objects.requireNonNull(server, "Server was NULL when broadcasting packet!");
        Objects.requireNonNull(packet, "Packet was NULL when broadcasting!");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isFancyMenuClient(player)) {
                sendToClient(player, packet);
            }
        }
    }

    public static void setSendToServerLogic(Consumer<String> dataConsumer) {
        sendToServerDataConsumer = dataConsumer;
    }

    public static void setSendToClientLogic(BiConsumer<ServerPlayer, String> playerAndDataConsumer) {
        sendToClientPlayerAndDataConsumer = playerAndDataConsumer;
    }

    /**
     * @param sender The sender of the packet in case it was sent from client to server. This is NULL if the packet was sent by the server to the client!
     * @param direction The direction the packet was sent to.
     * @param dataWithIdentifier The packet data, starting with the packet identifier.
     */
    public static void onPacketReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull String dataWithIdentifier) {
        int separatorIndex = dataWithIdentifier.indexOf(':');
        if (separatorIndex < 0) return;
        String packetIdentifier = dataWithIdentifier.substring(0, separatorIndex);
        PacketCodec<?> codec = PacketRegistry.getCodec(packetIdentifier);
        if (codec == null) {
            LOGGER.error("[FANCYMENU] No codec for packet data found with identifier: " + packetIdentifier, new NullPointerException("Codec returned for identifier was NULL!"));
            return;
        }
        if (direction == PacketDirection.TO_CLIENT) {
            Packet packet = deserializePacket(() -> Objects.requireNonNull(codec.deserialize(dataWithIdentifier.substring(separatorIndex + 1))));
            if (packet != null) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    try {
                        packet.processPacket(sender);
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to process packet on client!", ex);
                    }
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        } else if (direction == PacketDirection.TO_SERVER) {
            if (sender != null) {
                MinecraftServer server = sender.getServer();
                if (server != null) {
                    if (codec.getType() == HandshakePacket.class && !admitServerHandshake(sender)) return;
                    Packet packet = deserializePacket(() -> Objects.requireNonNull(codec.deserialize(dataWithIdentifier.substring(separatorIndex + 1))));
                    if (packet != null) {
                        sender.getServer().execute(() -> {
                            try {
                                packet.processPacket(sender);
                            } catch (Exception ex) {
                                LOGGER.error("[FANCYMENU] Failed to process packet on server!", ex);
                            }
                        });
                    }
                } else {
                    LOGGER.error("[FANCYMENU] Failed to process packet on server!", new NullPointerException("Server instance of sender was NULL!"));
                }
            } else {
                LOGGER.error("[FANCYMENU] Failed to process packet on server!", new NullPointerException("Sender was NULL!"));
            }
        }
    }

    private static boolean admitServerHandshake(@NotNull ServerPlayer sender) {
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.admitAttempt(Objects.requireNonNull(sender.connection));
        warnAboutRejectedHandshake(sender, decision);
        return decision.isAllowed();
    }

    private static void warnAboutRejectedHandshake(@NotNull ServerPlayer sender, @NotNull ServerHandshakeNegotiationTracker.Decision decision) {
        if (decision.isWarningRequired()) {
            LOGGER.warn("[FANCYMENU] Ignoring excessive or replayed handshake traffic from client: " + sender.getScoreboardName());
        }
    }

    @Nullable
    protected static Packet deserializePacket(@NotNull Supplier<Packet> packetSupplier) {
        try {
            return packetSupplier.get();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize packet!", ex);
        }
        return null;
    }

    public enum PacketDirection {
        TO_SERVER,
        TO_CLIENT
    }

}
