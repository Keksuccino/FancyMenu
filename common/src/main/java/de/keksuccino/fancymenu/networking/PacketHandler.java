package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeChunkReassembler;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgeProtocol;
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
import java.util.function.Supplier;

public class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final NetworkCapabilityLifecycle NETWORK_CAPABILITIES = new NetworkCapabilityLifecycle();
    private static final ServerHandshakeNegotiationTracker SERVER_HANDSHAKE_NEGOTIATIONS = new ServerHandshakeNegotiationTracker();
    private static final BridgeChunkReassembler BRIDGE_REASSEMBLER = new BridgeChunkReassembler();

    private static BridgeSendLogic<Connection> sendToServerLogic = null;
    private static BridgeSendLogic<ServerPlayer> sendToClientLogic = null;

    public static void onClientConnected(@NotNull Connection connection) {
        Connection exactConnection = Objects.requireNonNull(connection);
        if (NETWORK_CAPABILITIES.beginClientSession(exactConnection)) BRIDGE_REASSEMBLER.beginClientSession(exactConnection);
    }

    public static void onClientDisconnected(@Nullable Connection connection) {
        // Forge can emit a null logout while preparing a new connection or integrated server. Without an identity,
        // clearing here could erase a newer session; its exact login callback will replace any older state instead.
        if (NETWORK_CAPABILITIES.endClientSession(connection)) BRIDGE_REASSEMBLER.endSession(connection);
    }

    public static void onServerStarting(@NotNull MinecraftServer server) {
        NETWORK_CAPABILITIES.beginServerSession(Objects.requireNonNull(server));
    }

    public static void onServerPlayerConnected(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        MinecraftServer server = getServer(player);
        Object connection = Objects.requireNonNull(player.connection);
        if (NETWORK_CAPABILITIES.beginServerConnection(server, connection)) BRIDGE_REASSEMBLER.beginServerConnection(server, connection);
    }

    public static void onServerPlayerDisconnected(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        Object connection = Objects.requireNonNull(player.connection);
        NETWORK_CAPABILITIES.endServerConnection(getServer(player), connection);
        SERVER_HANDSHAKE_NEGOTIATIONS.remove(connection);
        BRIDGE_REASSEMBLER.endSession(connection);
    }

    public static void onServerStopped(@NotNull MinecraftServer server) {
        NETWORK_CAPABILITIES.endServerSession(Objects.requireNonNull(server));
        BRIDGE_REASSEMBLER.endServer(server);
    }

    public static boolean addFancyMenuServer(@NotNull Connection connection) {
        return addFancyMenuServer(connection, 0);
    }

    public static boolean addFancyMenuServer(@NotNull Connection connection, int bridgeProtocolVersion) {
        return NETWORK_CAPABILITIES.markClientServerCapable(Objects.requireNonNull(connection), bridgeProtocolVersion);
    }

    /**
     * @return true only for the first accepted handshake on this live play connection
     */
    public static boolean addFancyMenuClient(@NotNull ServerPlayer player) {
        return addFancyMenuClient(player, 0);
    }

    public static boolean addFancyMenuClient(@NotNull ServerPlayer player, int bridgeProtocolVersion) {
        Objects.requireNonNull(player);
        ServerHandshakeNegotiationTracker.Decision decision = SERVER_HANDSHAKE_NEGOTIATIONS.accept(Objects.requireNonNull(player.connection));
        warnAboutRejectedHandshake(player, decision);
        if (!decision.isAllowed()) return false;
        return NETWORK_CAPABILITIES.markServerClientCapable(getServer(player), player.connection, bridgeProtocolVersion);
    }

    public static boolean isFancyMenuClient(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player);
        return NETWORK_CAPABILITIES.isServerClientCapable(getServer(player), Objects.requireNonNull(player.connection));
    }

    public static void sendHandshakeToClient(@NotNull ServerPlayer player) {
        sendToClient(player, HandshakePacket.current());
    }

    public static void sendHandshakeToServer() {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection != null) sendHandshakeToServer(connection);
    }

    public static void sendHandshakeToServer(@NotNull Connection connection) {
        sendToServer(Objects.requireNonNull(connection), HandshakePacket.current());
    }

    public static <T extends Packet> void sendToServer(@NotNull T packet) {
        Connection connection = ClientPacketUtils.getConnectedConnection();
        if (connection != null) sendToServer(connection, packet);
    }

    private static <T extends Packet> void sendToServer(@NotNull Connection connection, @NotNull T packet) {
        if (!ClientPacketUtils.shouldSendToServer(packet, connection)) return;
        Objects.requireNonNull(sendToServerLogic, "Tried to send packet to server too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                String serialized = Objects.requireNonNull(codec.serialize(packet));
                BridgeMessageSender.SendResult result = sendToServerLogic.send(connection, serialized, NETWORK_CAPABILITIES.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION));
                reportSendResult(result, PacketDirection.TO_SERVER);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to send packet to server!", ex);
            }
        } else {
            LOGGER.error("[FANCYMENU] No codec found for packet: " + packet.getClass(), new NullPointerException("Codec returned for packet was NULL!"));
        }
    }

    public static <T extends Packet> void sendToClient(@NotNull ServerPlayer toPlayer, @NotNull T packet) {
        if (!(packet instanceof HandshakePacket) && !isFancyMenuClient(toPlayer)) return;
        Objects.requireNonNull(sendToClientLogic, "Tried to send packet to client too early! No logic set yet!");
        PacketCodec<T> codec = PacketRegistry.getCodecFor(Objects.requireNonNull(packet));
        if (codec != null) {
            try {
                String serialized = Objects.requireNonNull(codec.serialize(packet));
                BridgeMessageSender.SendResult result = sendToClientLogic.send(Objects.requireNonNull(toPlayer), serialized, NETWORK_CAPABILITIES.supportsServerBridgeProtocol(getServer(toPlayer), toPlayer.connection, BridgeProtocol.VERSION));
                reportSendResult(result, PacketDirection.TO_CLIENT);
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

    public static void setSendToServerLogic(BridgeSendLogic<Connection> logic) {
        sendToServerLogic = Objects.requireNonNull(logic);
    }

    public static void setSendToClientLogic(BridgeSendLogic<ServerPlayer> logic) {
        sendToClientLogic = Objects.requireNonNull(logic);
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
            if (packet instanceof HandshakePacket) {
                // The handshake chooses the codec for following ordered payloads. Deferring it until post-tick could
                // make chunks received later in the same network drain look unadvertised and discard valid data.
                try {
                    packet.processClientPacket(clientConnection);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to process handshake packet on client!", ex);
                }
            } else if (packet != null) {
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
                    if (packet instanceof HandshakePacket) {
                        // See the client branch above. Server-side handshake logic retains its server executor boundary
                        // for welcome-data work, while capability negotiation itself becomes visible immediately.
                        try {
                            packet.processPacket(sender);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] Failed to process handshake packet on server!", ex);
                        }
                    } else if (packet != null) {
                        server.execute(() -> {
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

    public static void onBridgeChunkReceived(@Nullable ServerPlayer sender, @NotNull PacketDirection direction, @NotNull BridgeChunkPayload payload, @Nullable Connection clientConnection) {
        Objects.requireNonNull(direction);
        Objects.requireNonNull(payload);
        if (direction == PacketDirection.TO_CLIENT) {
            if (clientConnection == null || !NETWORK_CAPABILITIES.supportsClientBridgeProtocol(clientConnection, BridgeProtocol.VERSION)) return;
            BridgeChunkReassembler.Result result = BRIDGE_REASSEMBLER.accept(clientConnection, payload);
            if (result.status() == BridgeChunkReassembler.Status.COMPLETE) onPacketReceived(null, direction, Objects.requireNonNull(result.message()), clientConnection);
            return;
        }
        if (sender == null) return;
        MinecraftServer server = sender.level().getServer();
        Object connection = sender.connection;
        if (server == null || !NETWORK_CAPABILITIES.supportsServerBridgeProtocol(server, connection, BridgeProtocol.VERSION)) return;
        BridgeChunkReassembler.Result result = BRIDGE_REASSEMBLER.accept(connection, payload);
        if (result.status() == BridgeChunkReassembler.Status.COMPLETE) onPacketReceived(sender, direction, Objects.requireNonNull(result.message()));
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

    private static void reportSendResult(@NotNull BridgeMessageSender.SendResult result, @NotNull PacketDirection direction) {
        switch (result) {
            case SENT -> {
            }
            case LEGACY_CHANNEL_UNAVAILABLE, CHUNK_CHANNEL_UNAVAILABLE -> LOGGER.debug("[FANCYMENU] Skipped " + direction + " bridge packet because the exact connection did not negotiate its payload channel.");
            case CHUNK_PROTOCOL_UNAVAILABLE -> LOGGER.warn("[FANCYMENU] Skipped oversized " + direction + " bridge packet because the exact peer did not advertise bridge protocol v1.");
            case MESSAGE_TOO_LARGE -> LOGGER.warn("[FANCYMENU] Skipped " + direction + " bridge packet because its encoded message exceeds 8 MiB.");
            case MALFORMED_TEXT -> LOGGER.warn("[FANCYMENU] Skipped " + direction + " bridge packet because its text contains an unpaired UTF-16 surrogate.");
            case INVALID_DIRECTION -> LOGGER.error("[FANCYMENU] Skipped bridge packet because its internal direction exceeds the legacy framing limit.");
        }
    }

    @NotNull
    private static MinecraftServer getServer(@NotNull ServerPlayer player) {
        return Objects.requireNonNull(player.level().getServer(), "Server instance of player was NULL!");
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
