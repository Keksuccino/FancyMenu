package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.handshake.HandshakePacket;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final NetworkCapabilityLifecycle NETWORK_CAPABILITIES = new NetworkCapabilityLifecycle();
    private static final ServerHandshakeNegotiationTracker SERVER_HANDSHAKE_NEGOTIATIONS = new ServerHandshakeNegotiationTracker();

    private static BiConsumer<Connection, String> sendToServerConnectionAndDataConsumer = null;
    private static BiConsumer<ServerPlayer, String> sendToClientPlayerAndDataConsumer = null;

    public static void onClientConnected(@NotNull Connection connection) {
        NETWORK_CAPABILITIES.beginClientSession(Objects.requireNonNull(connection));
    }

    public static void onClientDisconnected(@Nullable Connection connection) {
        // NeoForge can emit a null logout while preparing a new connection or integrated server. Without an identity,
        // clearing here could erase a newer session; its exact login callback will replace any older state instead.
        NETWORK_CAPABILITIES.endClientSession(connection);
    }

    public static void onServerStarting(@NotNull MinecraftServer server) {
        NETWORK_CAPABILITIES.beginServerSession(Objects.requireNonNull(server));
    }

    public static void onServerPlayerConnected(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        NETWORK_CAPABILITIES.beginServerConnection(getServer(player), Objects.requireNonNull(player.connection));
    }

    public static void onServerPlayerDisconnected(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        Object connection = Objects.requireNonNull(player.connection);
        NETWORK_CAPABILITIES.endServerConnection(getServer(player), connection);
        SERVER_HANDSHAKE_NEGOTIATIONS.remove(connection);
    }

    public static void onServerStopped(@NotNull MinecraftServer server) {
        NETWORK_CAPABILITIES.endServerSession(Objects.requireNonNull(server));
    }

    public static boolean addFancyMenuServer(@NotNull Connection connection) {
        return NETWORK_CAPABILITIES.markClientServerCapable(Objects.requireNonNull(connection));
    }

    /**
     * @return true only for the first accepted handshake on this live play connection
     */
    public static boolean addFancyMenuClient(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.accept(Objects.requireNonNull(player.connection));
        warnAboutRejectedHandshake(player, decision);
        if (!decision.isAllowed()) return false;
        return NETWORK_CAPABILITIES.markServerClientCapable(getServer(player), player.connection);
    }

    public static boolean isFancyMenuClient(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        return NETWORK_CAPABILITIES.isServerClientCapable(getServer(player), Objects.requireNonNull(player.connection));
    }

    public static void sendHandshakeToClient(@NotNull ServerPlayer player) {
        sendToClient(player, new HandshakePacket());
    }

    public static void sendHandshakeToServer() {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection != null) sendHandshakeToServer(connection);
    }

    public static void sendHandshakeToServer(@NotNull Connection connection) {
        sendToServer(Objects.requireNonNull(connection), new HandshakePacket());
    }

    public static <T extends Packet> void sendToServer(@NotNull T packet) {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection != null) sendToServer(connection, packet);
    }

    private static <T extends Packet> void sendToServer(@NotNull Connection connection, @NotNull T packet) {
        if (!ClientPacketUtils.shouldSendToServer(packet, connection)) return;
        Objects.requireNonNull(sendToServerConnectionAndDataConsumer, "Tried to send packet to server too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                sendToServerConnectionAndDataConsumer.accept(connection, Objects.requireNonNull(codec.serialize(packet)));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to send packet to server!", ex);
            }
        } else {
            LOGGER.error("[FANCYMENU] No codec found for packet: " + packet.getClass(), new NullPointerException("Codec returned for packet was NULL!"));
        }
    }

    public static <T extends Packet> void sendToClient(@NotNull ServerPlayer toPlayer, @NotNull T packet) {
        if (!(packet instanceof HandshakePacket) && !isFancyMenuClient(toPlayer)) return;
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

    public static void setSendToServerLogic(BiConsumer<Connection, String> connectionAndDataConsumer) {
        sendToServerConnectionAndDataConsumer = connectionAndDataConsumer;
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
        // This compatibility overload is safe for serverbound packets only. A clientbound receiver must provide the
        // exact receiving connection; inferring Minecraft's current connection could attribute a delayed old packet
        // to a replacement session.
        onPacketReceived(sender, direction, dataWithIdentifier, null);
    }

    public static void onPacketReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull String dataWithIdentifier, @Nullable Connection clientConnection) {
        int separatorIndex = dataWithIdentifier.indexOf(':');
        if (separatorIndex < 0) return;
        String packetIdentifier = dataWithIdentifier.substring(0, separatorIndex);
        PacketCodec<?> codec = PacketRegistry.getCodec(packetIdentifier);
        if (codec == null) {
            LOGGER.error("[FANCYMENU] No codec for packet data found with identifier: " + packetIdentifier, new NullPointerException("Codec returned for identifier was NULL!"));
            return;
        }
        if (direction == PacketDirection.TO_CLIENT) {
            if (clientConnection == null || !NETWORK_CAPABILITIES.isClientSessionActive(clientConnection)) return;
            Packet packet = deserializePacket(() -> Objects.requireNonNull(codec.deserialize(dataWithIdentifier.substring(separatorIndex + 1))));
            if (packet != null) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    if (!NETWORK_CAPABILITIES.isClientSessionActive(clientConnection)) return;
                    try {
                        packet.processClientPacket(clientConnection);
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

    static boolean isClientSessionActive(@NotNull Connection connection) {
        return NETWORK_CAPABILITIES.isClientSessionActive(Objects.requireNonNull(connection));
    }

    static boolean isFancyMenuServer(@NotNull Connection connection) {
        return NETWORK_CAPABILITIES.isClientServerCapable(Objects.requireNonNull(connection));
    }

    @NotNull
    private static MinecraftServer getServer(@NotNull ServerPlayer player) {
        return Objects.requireNonNull(player.getLevel().getServer(), "Server instance of player was NULL!");
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
