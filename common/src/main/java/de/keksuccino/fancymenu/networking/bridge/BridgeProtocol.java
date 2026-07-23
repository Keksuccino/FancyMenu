package de.keksuccino.fancymenu.networking.bridge;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Shared limits and strict UTF-8 helpers for both bridge wire formats.
 */
public final class BridgeProtocol {

    public static final int VERSION = 1;
    public static final int MAX_LEGACY_MESSAGE_BYTES = 30 * 1024;
    public static final int MAX_CHUNK_DATA_BYTES = 30 * 1024;
    public static final int MAX_LOGICAL_MESSAGE_BYTES = 8 * 1024 * 1024;
    public static final int MAX_LEGACY_DIRECTION_BYTES = 32;
    public static final int MAX_PAYLOAD_BODY_BYTES = 32766;

    private BridgeProtocol() {
    }

    static int encodedLength(@NotNull CharSequence value, int maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("Maximum byte length must not be negative");
        int encodedLength = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x80) {
                encodedLength++;
            } else if (current < 0x800) {
                encodedLength += 2;
            } else if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) throw new MalformedTextException();
                encodedLength += 4;
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw new MalformedTextException();
            } else {
                encodedLength += 3;
            }
            if (encodedLength > maxBytes) throw new EncodedLengthExceededException(maxBytes);
        }
        return encodedLength;
    }

    static byte @NotNull [] encode(@NotNull String value, int expectedLength) {
        int actualLength = encodedLength(value, expectedLength);
        if (actualLength != expectedLength) throw new IllegalArgumentException("Expected UTF-8 length does not match the value");
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length != expectedLength) throw new IllegalStateException("UTF-8 encoder disagreed with the validated byte length");
        return encoded;
    }

    static @NotNull String decode(@NotNull ByteBuffer encoded) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(encoded).toString();
    }

    static @NotNull String decodeChunks(byte @NotNull [] @NotNull [] chunks, int totalEncodedLength) throws CharacterCodingException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        StringBuilder decoded = new StringBuilder(totalEncodedLength);
        CharBuffer characters = CharBuffer.allocate(8192);
        byte[] carry = new byte[0];
        for (int chunkIndex = 0; chunkIndex < chunks.length; chunkIndex++) {
            byte[] chunk = chunks[chunkIndex];
            ByteBuffer encoded;
            if (carry.length == 0) {
                encoded = ByteBuffer.wrap(chunk);
            } else {
                byte[] joined = new byte[carry.length + chunk.length];
                System.arraycopy(carry, 0, joined, 0, carry.length);
                System.arraycopy(chunk, 0, joined, carry.length, chunk.length);
                encoded = ByteBuffer.wrap(joined);
            }
            boolean finalChunk = chunkIndex == chunks.length - 1;
            while (true) {
                CoderResult result = decoder.decode(encoded, characters, finalChunk);
                appendDecoded(decoded, characters);
                if (result.isError()) result.throwException();
                if (result.isUnderflow()) break;
            }
            if (!finalChunk && encoded.hasRemaining()) {
                carry = new byte[encoded.remaining()];
                encoded.get(carry);
            } else {
                carry = new byte[0];
            }
        }
        while (true) {
            CoderResult result = decoder.flush(characters);
            appendDecoded(decoded, characters);
            if (result.isError()) result.throwException();
            if (result.isUnderflow()) break;
        }
        return decoded.toString();
    }

    private static void appendDecoded(@NotNull StringBuilder target, @NotNull CharBuffer characters) {
        characters.flip();
        target.append(characters);
        characters.clear();
    }

    static int varIntSize(int value) {
        if (value < 0) throw new IllegalArgumentException("VarInt value must not be negative");
        int size = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            size++;
        }
        return size;
    }

    static final class EncodedLengthExceededException extends IllegalArgumentException {

        private EncodedLengthExceededException(int maximum) {
            super("Encoded UTF-8 data exceeds " + maximum + " bytes");
        }
    }

    static final class MalformedTextException extends IllegalArgumentException {

        private MalformedTextException() {
            super("Text contains an unpaired UTF-16 surrogate");
        }
    }
}
