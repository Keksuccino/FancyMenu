package de.keksuccino.fancymenu.networking.bridge;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

final class BridgeWireCodec {

    private BridgeWireCodec() {
    }

    static @NotNull String readUtf8(@NotNull FriendlyByteBuf byteBuf, int maxEncodedBytes) {
        int encodedLength = byteBuf.readVarInt();
        if (encodedLength < 0 || encodedLength > maxEncodedBytes) throw new DecoderException("Bridge UTF-8 field has an invalid encoded length");
        if (encodedLength > byteBuf.readableBytes()) throw new DecoderException("Bridge UTF-8 field is truncated");
        ByteBuffer encoded = byteBuf.nioBuffer(byteBuf.readerIndex(), encodedLength);
        try {
            String value = BridgeProtocol.decode(encoded);
            byteBuf.skipBytes(encodedLength);
            return value;
        } catch (CharacterCodingException ex) {
            throw new DecoderException("Bridge UTF-8 field is malformed", ex);
        }
    }

    static void writeUtf8(@NotNull FriendlyByteBuf byteBuf, @NotNull String value, int encodedLength) {
        byteBuf.writeVarInt(encodedLength);
        int written = ByteBufUtil.writeUtf8(byteBuf, value);
        if (written != encodedLength) throw new IllegalStateException("UTF-8 encoder disagreed with the validated byte length");
    }
}
