package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Accumulates raw WebSocket text bytes so an UTF-8 code point may safely span continuation frames. The byte and
 * fragment limits are checked before copying, and every terminal result releases the retained array immediately.
 */
final class InboundMessageBuffer {

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final int maxMessageUtf8Bytes;
    private final int maxFragments;

    private MessageType activeMessageType = MessageType.NONE;
    private byte[] textBytes = EMPTY_BYTES;
    private int messageBytes;
    private int fragmentCount;

    InboundMessageBuffer(int maxMessageUtf8Bytes, int maxFragments) {
        if (maxMessageUtf8Bytes <= 0) {
            throw new IllegalArgumentException("maxMessageUtf8Bytes must be positive");
        }
        if (maxFragments <= 0) {
            throw new IllegalArgumentException("maxFragments must be positive");
        }
        this.maxMessageUtf8Bytes = maxMessageUtf8Bytes;
        this.maxFragments = maxFragments;
    }

    synchronized @NotNull Result acceptText(@NotNull ByteBuffer data, boolean finalFragment) {
        if (this.activeMessageType != MessageType.NONE) {
            return fail(ResultType.INVALID_SEQUENCE);
        }
        this.activeMessageType = MessageType.TEXT;
        return acceptData(data, finalFragment);
    }

    synchronized @NotNull Result acceptBinary(@NotNull ByteBuffer data, boolean finalFragment) {
        if (this.activeMessageType != MessageType.NONE) {
            return fail(ResultType.INVALID_SEQUENCE);
        }
        this.activeMessageType = MessageType.BINARY;
        return acceptData(data, finalFragment);
    }

    synchronized @NotNull Result acceptContinuation(@NotNull ByteBuffer data, boolean finalFragment) {
        if (this.activeMessageType == MessageType.NONE) {
            return fail(ResultType.INVALID_SEQUENCE);
        }
        return acceptData(data, finalFragment);
    }

    synchronized void reset() {
        resetInternal();
    }

    synchronized int bufferedBytes() {
        return this.messageBytes;
    }

    synchronized int bufferedFragments() {
        return this.fragmentCount;
    }

    private @NotNull Result acceptData(@NotNull ByteBuffer source, boolean finalFragment) {
        int fragmentBytes = source.remaining();
        if (this.fragmentCount >= this.maxFragments) {
            return fail(ResultType.TOO_MANY_FRAGMENTS);
        }
        if (fragmentBytes > this.maxMessageUtf8Bytes - this.messageBytes) {
            return fail(ResultType.TOO_LARGE);
        }

        this.fragmentCount++;
        if (this.activeMessageType == MessageType.TEXT && fragmentBytes > 0) {
            ensureTextCapacity(this.messageBytes + fragmentBytes);
            ByteBuffer readable = source.duplicate();
            readable.get(this.textBytes, this.messageBytes, fragmentBytes);
        }
        this.messageBytes += fragmentBytes;

        if (!finalFragment) {
            return new Result(ResultType.PARTIAL, null, 0);
        }

        int completedMessageBytes = this.messageBytes;
        if (this.activeMessageType == MessageType.BINARY) {
            resetInternal();
            return new Result(ResultType.COMPLETE_BINARY, null, completedMessageBytes);
        }

        try {
            String completedText = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(this.textBytes, 0, completedMessageBytes)).toString();
            resetInternal();
            return new Result(ResultType.COMPLETE_TEXT, completedText, completedMessageBytes);
        } catch (CharacterCodingException ex) {
            return fail(ResultType.INVALID_UTF8);
        }
    }

    private void ensureTextCapacity(int requiredCapacity) {
        if (requiredCapacity <= this.textBytes.length) {
            return;
        }
        int grownCapacity = Math.max(256, this.textBytes.length + (this.textBytes.length >> 1));
        int newCapacity = Math.min(this.maxMessageUtf8Bytes, Math.max(requiredCapacity, grownCapacity));
        this.textBytes = Arrays.copyOf(this.textBytes, newCapacity);
    }

    private @NotNull Result fail(@NotNull ResultType resultType) {
        resetInternal();
        return new Result(resultType, null, 0);
    }

    private void resetInternal() {
        this.activeMessageType = MessageType.NONE;
        this.textBytes = EMPTY_BYTES;
        this.messageBytes = 0;
        this.fragmentCount = 0;
    }

    enum ResultType {
        PARTIAL,
        COMPLETE_TEXT,
        COMPLETE_BINARY,
        TOO_LARGE,
        TOO_MANY_FRAGMENTS,
        INVALID_UTF8,
        INVALID_SEQUENCE
    }

    record Result(@NotNull ResultType type, @Nullable String text, int utf8Bytes) {
    }

    private enum MessageType {
        NONE,
        TEXT,
        BINARY
    }
}
