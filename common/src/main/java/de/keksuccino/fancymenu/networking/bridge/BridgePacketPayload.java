package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Shared legacy bridge wire format for every loader.
 * <p>
 * The former Fabric-only {@code minecraft:fancymenu_packet_bridge} identifier is intentionally not retained as an
 * alias. Forge rejects mod payload registrations in the {@code minecraft} namespace, so keeping that alias only on
 * Fabric would recreate a loader-specific protocol and make channel negotiation ambiguous again.
 */
public final class BridgePacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("fancymenu", "fancymenu_bridge_packet");
    public static final String TO_SERVER_WIRE_DIRECTION = "server";
    public static final String TO_CLIENT_WIRE_DIRECTION = "client";

    /**
     * This field is retained for wire compatibility only. Receivers must use the direction supplied by their
     * loader callback, because a remote peer controls the serialized value.
     */
    @Nullable private final String direction;
    @Nullable private final String dataWithIdentifier;
    private final boolean valid;

    public BridgePacketPayload(@NotNull String direction, @NotNull String dataWithIdentifier) {
        validate(Objects.requireNonNull(direction), Objects.requireNonNull(dataWithIdentifier));
        this.direction = direction;
        this.dataWithIdentifier = dataWithIdentifier;
        this.valid = true;
    }

    public BridgePacketPayload(@NotNull FriendlyByteBuf byteBuf) {
        DecodedFields decoded = decode(Objects.requireNonNull(byteBuf));
        this.direction = decoded.direction;
        this.dataWithIdentifier = decoded.dataWithIdentifier;
        this.valid = decoded.valid;
    }

    public void write(@NotNull FriendlyByteBuf byteBuf) {
        if (!this.valid || this.direction == null || this.dataWithIdentifier == null) throw new IllegalStateException("Cannot encode an invalid legacy bridge payload");
        Lengths lengths = validate(this.direction, this.dataWithIdentifier);
        BridgeWireCodec.writeUtf8(byteBuf, this.direction, lengths.directionBytes);
        BridgeWireCodec.writeUtf8(byteBuf, this.dataWithIdentifier, lengths.messageBytes);
    }

    private static @NotNull DecodedFields decode(@NotNull FriendlyByteBuf byteBuf) {
        int bodyEnd = byteBuf.writerIndex();
        try {
            if (byteBuf.readableBytes() > BridgeProtocol.MAX_PAYLOAD_BODY_BYTES) return DecodedFields.invalid();
            String direction = BridgeWireCodec.readUtf8(byteBuf, BridgeProtocol.MAX_LEGACY_DIRECTION_BYTES);
            String dataWithIdentifier = BridgeWireCodec.readUtf8(byteBuf, BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES);
            if (byteBuf.isReadable()) return DecodedFields.invalid();
            validate(direction, dataWithIdentifier);
            return new DecodedFields(direction, dataWithIdentifier, true);
        } catch (RuntimeException ignored) {
            return DecodedFields.invalid();
        } finally {
            // A malformed registered payload must not leave unread attacker-controlled bytes for another decoder.
            byteBuf.readerIndex(bodyEnd);
        }
    }

    private static @NotNull Lengths validate(@NotNull String direction, @NotNull String dataWithIdentifier) {
        int directionBytes = BridgeProtocol.encodedLength(direction, BridgeProtocol.MAX_LEGACY_DIRECTION_BYTES);
        int messageBytes = BridgeProtocol.encodedLength(dataWithIdentifier, BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES);
        int bodyBytes = BridgeProtocol.varIntSize(directionBytes) + directionBytes + BridgeProtocol.varIntSize(messageBytes) + messageBytes;
        if (bodyBytes > BridgeProtocol.MAX_PAYLOAD_BODY_BYTES) throw new IllegalArgumentException("Legacy bridge payload body exceeds the protocol limit");
        return new Lengths(directionBytes, messageBytes);
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction) {
        if (this.valid && this.dataWithIdentifier != null) PacketHandler.onPacketReceived(sender, direction, this.dataWithIdentifier);
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction, @Nullable Connection clientConnection) {
        if (this.valid && this.dataWithIdentifier != null) PacketHandler.onPacketReceived(sender, direction, this.dataWithIdentifier, clientConnection);
    }

    public @Nullable String direction() {
        return this.direction;
    }

    public @Nullable String dataWithIdentifier() {
        return this.dataWithIdentifier;
    }

    public boolean isValid() {
        return this.valid;
    }

    private record Lengths(int directionBytes, int messageBytes) {
    }

    private record DecodedFields(@Nullable String direction, @Nullable String dataWithIdentifier, boolean valid) {

        private static @NotNull DecodedFields invalid() {
            return new DecodedFields(null, null, false);
        }
    }
}
