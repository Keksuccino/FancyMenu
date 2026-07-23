package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InboundMessageBufferTest {

    @Test
    void acceptsExactByteLimitAndRejectsOneByteOver() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(4, 4);

        InboundMessageBuffer.Result exact = buffer.acceptText(bytes("test"), true);

        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_TEXT, exact.type());
        assertEquals("test", exact.text());
        assertEquals(4, exact.utf8Bytes());
        assertReset(buffer);

        InboundMessageBuffer.Result oversized = buffer.acceptText(bytes("tests"), true);

        assertEquals(InboundMessageBuffer.ResultType.TOO_LARGE, oversized.type());
        assertNull(oversized.text());
        assertReset(buffer);
    }

    @Test
    void decodesMultibyteCodePointSplitAcrossContinuationFrames() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(4, 2);
        byte[] emoji = "😀".getBytes(StandardCharsets.UTF_8);

        InboundMessageBuffer.Result first = buffer.acceptText(ByteBuffer.wrap(emoji, 0, 2), false);
        InboundMessageBuffer.Result second = buffer.acceptContinuation(ByteBuffer.wrap(emoji, 2, 2), true);

        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, first.type());
        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_TEXT, second.type());
        assertEquals("😀", second.text());
        assertEquals(4, second.utf8Bytes());
        assertReset(buffer);
    }

    @Test
    void rejectsInvalidUtf8AndReleasesPartialState() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(8, 2);

        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptText(ByteBuffer.wrap(new byte[]{(byte) 0xF0}), false).type());
        assertEquals(InboundMessageBuffer.ResultType.INVALID_UTF8, buffer.acceptContinuation(ByteBuffer.wrap(new byte[]{0x28}), true).type());
        assertReset(buffer);

        InboundMessageBuffer.Result next = buffer.acceptText(bytes("ok"), true);
        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_TEXT, next.type());
        assertEquals("ok", next.text());
    }

    @Test
    void zeroByteFragmentsStillCountTowardFragmentLimit() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(8, 2);

        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptText(ByteBuffer.allocate(0), false).type());
        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptContinuation(ByteBuffer.allocate(0), false).type());
        assertEquals(2, buffer.bufferedFragments());

        assertEquals(InboundMessageBuffer.ResultType.TOO_MANY_FRAGMENTS, buffer.acceptContinuation(ByteBuffer.allocate(0), true).type());
        assertReset(buffer);
    }

    @Test
    void binaryCompletionReleasesStateBeforeTheNextTextMessage() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(4, 3);

        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptBinary(ByteBuffer.wrap(new byte[]{1, 2}), false).type());
        InboundMessageBuffer.Result binary = buffer.acceptContinuation(ByteBuffer.wrap(new byte[]{3, 4}), true);

        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_BINARY, binary.type());
        assertEquals(4, binary.utf8Bytes());
        assertReset(buffer);

        InboundMessageBuffer.Result text = buffer.acceptText(bytes("next"), true);
        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_TEXT, text.type());
        assertEquals("next", text.text());
    }

    @Test
    void invalidFragmentSequenceIsTerminalAndResettable() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(8, 2);

        assertEquals(InboundMessageBuffer.ResultType.INVALID_SEQUENCE, buffer.acceptContinuation(bytes("x"), true).type());
        assertReset(buffer);
        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptText(bytes("a"), false).type());
        assertEquals(InboundMessageBuffer.ResultType.INVALID_SEQUENCE, buffer.acceptBinary(bytes("b"), true).type());
        assertReset(buffer);
    }

    @Test
    void explicitTerminalResetReleasesAnIncompleteMessage() {
        InboundMessageBuffer buffer = new InboundMessageBuffer(8, 2);
        assertEquals(InboundMessageBuffer.ResultType.PARTIAL, buffer.acceptText(bytes("partial"), false).type());
        assertEquals(7, buffer.bufferedBytes());

        buffer.reset();

        assertReset(buffer);
        InboundMessageBuffer.Result next = buffer.acceptText(bytes("new"), true);
        assertEquals(InboundMessageBuffer.ResultType.COMPLETE_TEXT, next.type());
        assertEquals("new", next.text());
    }

    private static ByteBuffer bytes(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertReset(InboundMessageBuffer buffer) {
        assertEquals(0, buffer.bufferedBytes());
        assertEquals(0, buffer.bufferedFragments());
    }
}
