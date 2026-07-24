package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkRemoteWebSocketTransportTest {

    @Test
    void constructorRejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteWebSocketTransport(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteWebSocketTransport(1, 0));
    }

    @Test
    void exactUtf8LimitCompletesAndResets() {
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer buffer = new JdkRemoteWebSocketTransport.JdkInboundTextBuffer(8, 2);
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer.Result result = buffer.accept("12345678", true);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.COMPLETE, result.type());
        assertEquals("12345678", result.text());
        assertEquals(8, result.utf8Bytes());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.COMPLETE, buffer.accept("next", true).type());
    }

    @Test
    void fragmentedSurrogatePairIsCountedAsOneCodePoint() {
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer buffer = new JdkRemoteWebSocketTransport.JdkInboundTextBuffer(4, 2);
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer.Result first = buffer.accept("\uD83D", false);
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer.Result second = buffer.accept("\uDE00", true);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.PARTIAL, first.type());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.COMPLETE, second.type());
        assertEquals("😀", second.text());
        assertEquals(4, second.utf8Bytes());
    }

    @Test
    void aggregateLargerThanByteLimitIsRejectedAndReleased() {
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer buffer = new JdkRemoteWebSocketTransport.JdkInboundTextBuffer(4, 2);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.PARTIAL, buffer.accept("123", false).type());
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer.Result rejected = buffer.accept("45", true);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.TOO_LARGE, rejected.type());
        assertNull(rejected.text());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.COMPLETE, buffer.accept("ok", true).type());
    }

    @Test
    void emptyFragmentsStillConsumeFragmentBudget() {
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer buffer = new JdkRemoteWebSocketTransport.JdkInboundTextBuffer(8, 2);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.PARTIAL, buffer.accept("", false).type());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.PARTIAL, buffer.accept("", false).type());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.TOO_MANY_FRAGMENTS, buffer.accept("", true).type());
    }

    @Test
    void malformedUtf16IsRejected() {
        JdkRemoteWebSocketTransport.JdkInboundTextBuffer buffer = new JdkRemoteWebSocketTransport.JdkInboundTextBuffer(8, 2);
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.INVALID_UTF16, buffer.accept("\uDE00", true).type());
        assertEquals(JdkRemoteWebSocketTransport.JdkInboundTextBuffer.ResultType.INVALID_UTF16, buffer.accept("\uD83D", true).type());
    }

    @Test
    void shutdownIsIdempotentTerminatesExecutorAndRejectsLateConnections() {
        JdkRemoteWebSocketTransport transport = new JdkRemoteWebSocketTransport(1024, 4);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        RemoteWebSocketTransport.Listener listener = new RemoteWebSocketTransport.Listener() {
            @Override public void onOpen(RemoteWebSocketTransport.Connection connection) {}
            @Override public void onText(RemoteWebSocketTransport.Connection connection, String data, int utf8Bytes) {}
            @Override public void onPong(RemoteWebSocketTransport.Connection connection) {}
            @Override public void onClose(RemoteWebSocketTransport.Connection connection, int statusCode, String reason) {}
            @Override public void onError(RemoteWebSocketTransport.Connection connection, Throwable error) { failure.set(error); }
        };

        transport.shutdown();
        transport.shutdown();
        RemoteWebSocketTransport.Connection rejected = transport.connect(URI.create("ws://127.0.0.1:1/socket"), listener);

        assertTrue(transport.isTerminated());
        assertFalse(rejected.isOpen());
        assertInstanceOf(RejectedExecutionException.class, failure.get());
    }
}
