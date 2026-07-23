package de.keksuccino.fancymenu.networking.bridge;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Binary v1 bridge payload. Chunk boundaries are byte boundaries; UTF-8 is decoded only after exact reassembly.
 */
public final class BridgeChunkPayload implements CustomPacketPayload {

    public static final Type<BridgeChunkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("fancymenu", "fancymenu_bridge_chunk"));
    public static final StreamCodec<FriendlyByteBuf, BridgeChunkPayload> CODEC = CustomPacketPayload.codec(BridgeChunkPayload::write, BridgeChunkPayload::decode);
    static final int HEADER_BYTES = 33;
    static final int MAX_CHUNK_COUNT = (BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES + BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1) / BridgeProtocol.MAX_CHUNK_DATA_BYTES;

    @Nullable private final UUID transferId;
    private final int totalLength;
    private final int chunkIndex;
    private final int chunkCount;
    private final byte[] chunkData;
    private final boolean valid;

    public BridgeChunkPayload(@NotNull UUID transferId, int totalLength, int chunkIndex, int chunkCount, byte @NotNull [] chunkData) {
        this(Objects.requireNonNull(transferId), totalLength, chunkIndex, chunkCount, Objects.requireNonNull(chunkData), true, true);
    }

    private BridgeChunkPayload(@Nullable UUID transferId, int totalLength, int chunkIndex, int chunkCount, byte @NotNull [] chunkData, boolean valid, boolean copyData) {
        if (valid) validateMetadata(totalLength, chunkIndex, chunkCount, chunkData.length);
        this.transferId = transferId;
        this.totalLength = totalLength;
        this.chunkIndex = chunkIndex;
        this.chunkCount = chunkCount;
        this.chunkData = copyData ? Arrays.copyOf(chunkData, chunkData.length) : chunkData;
        this.valid = valid;
    }

    private static @NotNull BridgeChunkPayload invalid(@Nullable UUID transferId) {
        return new BridgeChunkPayload(transferId, 0, 0, 0, new byte[0], false, false);
    }

    static @NotNull BridgeChunkPayload trusted(@NotNull UUID transferId, int totalLength, int chunkIndex, int chunkCount, byte @NotNull [] chunkData) {
        return new BridgeChunkPayload(Objects.requireNonNull(transferId), totalLength, chunkIndex, chunkCount, Objects.requireNonNull(chunkData), true, false);
    }

    private static @NotNull BridgeChunkPayload decode(@NotNull FriendlyByteBuf byteBuf) {
        int bodyEnd = byteBuf.writerIndex();
        UUID transferId = null;
        try {
            int bodyLength = byteBuf.readableBytes();
            if (bodyLength < HEADER_BYTES || bodyLength > HEADER_BYTES + BridgeProtocol.MAX_CHUNK_DATA_BYTES || bodyLength > BridgeProtocol.MAX_PAYLOAD_BODY_BYTES) return invalid(null);
            int version = byteBuf.readUnsignedByte();
            transferId = new UUID(byteBuf.readLong(), byteBuf.readLong());
            int totalLength = byteBuf.readInt();
            int chunkIndex = byteBuf.readInt();
            int chunkCount = byteBuf.readInt();
            int chunkLength = byteBuf.readInt();
            if (version != BridgeProtocol.VERSION || chunkLength != byteBuf.readableBytes()) return invalid(transferId);
            validateMetadata(totalLength, chunkIndex, chunkCount, chunkLength);
            byte[] chunkData = new byte[chunkLength];
            byteBuf.readBytes(chunkData);
            return new BridgeChunkPayload(transferId, totalLength, chunkIndex, chunkCount, chunkData, true, false);
        } catch (RuntimeException ignored) {
            return invalid(transferId);
        } finally {
            // Invalid headers and lengths are terminal for this payload body and are always consumed in full.
            byteBuf.readerIndex(bodyEnd);
        }
    }

    private void write(@NotNull FriendlyByteBuf byteBuf) {
        if (!this.valid || this.transferId == null) throw new IllegalStateException("Cannot encode an invalid bridge chunk payload");
        validateMetadata(this.totalLength, this.chunkIndex, this.chunkCount, this.chunkData.length);
        byteBuf.writeByte(BridgeProtocol.VERSION);
        byteBuf.writeLong(this.transferId.getMostSignificantBits());
        byteBuf.writeLong(this.transferId.getLeastSignificantBits());
        byteBuf.writeInt(this.totalLength);
        byteBuf.writeInt(this.chunkIndex);
        byteBuf.writeInt(this.chunkCount);
        byteBuf.writeInt(this.chunkData.length);
        byteBuf.writeBytes(this.chunkData);
    }

    static void validateMetadata(int totalLength, int chunkIndex, int chunkCount, int chunkLength) {
        if (totalLength <= BridgeProtocol.MAX_LEGACY_MESSAGE_BYTES || totalLength > BridgeProtocol.MAX_LOGICAL_MESSAGE_BYTES) throw new IllegalArgumentException("Bridge chunk total length is outside the v1 range");
        int expectedChunkCount = (totalLength + BridgeProtocol.MAX_CHUNK_DATA_BYTES - 1) / BridgeProtocol.MAX_CHUNK_DATA_BYTES;
        if (chunkCount != expectedChunkCount || chunkCount < 2 || chunkCount > MAX_CHUNK_COUNT) throw new IllegalArgumentException("Bridge chunk count does not match its total length");
        if (chunkIndex < 0 || chunkIndex >= chunkCount) throw new IllegalArgumentException("Bridge chunk index is outside its transfer");
        int expectedChunkLength = chunkIndex == chunkCount - 1 ? totalLength - chunkIndex * BridgeProtocol.MAX_CHUNK_DATA_BYTES : BridgeProtocol.MAX_CHUNK_DATA_BYTES;
        if (chunkLength != expectedChunkLength || chunkLength < 1 || chunkLength > BridgeProtocol.MAX_CHUNK_DATA_BYTES) throw new IllegalArgumentException("Bridge chunk data length does not match its position");
        if (HEADER_BYTES + chunkLength > BridgeProtocol.MAX_PAYLOAD_BODY_BYTES) throw new IllegalArgumentException("Bridge chunk payload body exceeds the protocol limit");
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction) {
        PacketHandler.onBridgeChunkReceived(sender, direction, this, null);
    }

    public void handle(@Nullable ServerPlayer sender, @NotNull PacketHandler.PacketDirection direction, @Nullable Connection clientConnection) {
        PacketHandler.onBridgeChunkReceived(sender, direction, this, clientConnection);
    }

    public @Nullable UUID transferId() {
        return this.transferId;
    }

    public int totalLength() {
        return this.totalLength;
    }

    public int chunkIndex() {
        return this.chunkIndex;
    }

    public int chunkCount() {
        return this.chunkCount;
    }

    public byte @NotNull [] chunkData() {
        return Arrays.copyOf(this.chunkData, this.chunkData.length);
    }

    byte @NotNull [] chunkDataUnsafe() {
        return this.chunkData;
    }

    public boolean isValid() {
        return this.valid;
    }

    @Override
    public @NotNull Type<BridgeChunkPayload> type() {
        return TYPE;
    }
}
