package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkRemoteWebSocketTransportTest {

    @Test
    void exactLimitFragmentedTextIsDeliveredOnce() {
        Harness harness = new Harness(8, 2);
        CountingListener listener = new CountingListener();
        RemoteWebSocketTransport.Connection connection = harness.connect(listener);

        harness.open();
        harness.listener.get().onText(harness.webSocket, "1234", false);
        harness.listener.get().onText(harness.webSocket, "5678", true);

        assertTrue(connection.isOpen());
        assertEquals("12345678", listener.text.get());
        assertEquals(8, listener.utf8Bytes.get());
        assertEquals(1, listener.textCallbacks.get());
        assertEquals(0, listener.closeCallbacks.get());
    }

    @Test
    void oversizedTextIsRejectedWithoutRetainingOrDeliveringIt() {
        Harness harness = new Harness(4, 2);
        CountingListener listener = new CountingListener();
        RemoteWebSocketTransport.Connection connection = harness.connect(listener);

        harness.open();
        harness.listener.get().onText(harness.webSocket, "12345", true);

        assertFalse(connection.isOpen());
        assertEquals(JdkRemoteWebSocketTransport.MESSAGE_TOO_BIG_STATUS, harness.webSocket.closeStatus);
        assertEquals(JdkRemoteWebSocketTransport.MESSAGE_TOO_BIG_STATUS, listener.closeStatus.get());
        assertEquals(0, listener.textCallbacks.get());
        assertEquals(1, listener.closeCallbacks.get());
    }

    @Test
    void excessiveZeroLengthFragmentsAreRejected() {
        Harness harness = new Harness(8, 2);
        CountingListener listener = new CountingListener();
        harness.connect(listener);

        harness.open();
        harness.listener.get().onText(harness.webSocket, "", false);
        harness.listener.get().onText(harness.webSocket, "", false);
        harness.listener.get().onText(harness.webSocket, "", true);

        assertEquals(JdkRemoteWebSocketTransport.MESSAGE_TOO_BIG_STATUS, harness.webSocket.closeStatus);
        assertEquals(0, listener.textCallbacks.get());
        assertEquals(1, listener.closeCallbacks.get());
    }

    @Test
    void malformedUtf16TextIsRejectedAsInvalidPayload() {
        Harness harness = new Harness(8, 2);
        CountingListener listener = new CountingListener();
        harness.connect(listener);

        harness.open();
        harness.listener.get().onText(harness.webSocket, "\uD800", true);

        assertEquals(JdkRemoteWebSocketTransport.INVALID_PAYLOAD_STATUS, harness.webSocket.closeStatus);
        assertEquals(0, listener.textCallbacks.get());
        assertEquals(1, listener.closeCallbacks.get());
    }

    @Test
    void outboundOperationsDelegateToTheJdkSocket() {
        Harness harness = new Harness(8, 2);
        CountingListener listener = new CountingListener();
        RemoteWebSocketTransport.Connection connection = harness.connect(listener);

        harness.open();
        connection.sendText("outbound").join();
        connection.sendPing(new byte[]{1, 2, 3}).join();
        connection.close(1000, "done").join();

        assertEquals("outbound", harness.webSocket.sentText);
        assertArrayEquals(new byte[]{1, 2, 3}, harness.webSocket.sentPing);
        assertEquals(1000, harness.webSocket.closeStatus);
        assertEquals("done", harness.webSocket.closeReason);
    }

    private static final class Harness {

        private final AtomicReference<WebSocket.Listener> listener = new AtomicReference<>();
        private final FakeWebSocket webSocket = new FakeWebSocket();
        private final JdkRemoteWebSocketTransport transport;

        private Harness(int maxInboundMessageUtf8Bytes, int maxInboundMessageFragments) {
            this.transport = new JdkRemoteWebSocketTransport(maxInboundMessageUtf8Bytes, maxInboundMessageFragments, (uri, listener) -> {
                this.listener.set(listener);
                return CompletableFuture.completedFuture(this.webSocket);
            });
        }

        private RemoteWebSocketTransport.Connection connect(CountingListener listener) {
            return this.transport.connect(URI.create("ws://127.0.0.1/test"), listener);
        }

        private void open() {
            this.listener.get().onOpen(this.webSocket);
        }
    }

    private static final class CountingListener implements RemoteWebSocketTransport.Listener {

        private final AtomicReference<String> text = new AtomicReference<>();
        private final AtomicInteger utf8Bytes = new AtomicInteger();
        private final AtomicInteger textCallbacks = new AtomicInteger();
        private final AtomicInteger closeCallbacks = new AtomicInteger();
        private final AtomicInteger closeStatus = new AtomicInteger(-1);

        @Override
        public void onOpen(@NotNull RemoteWebSocketTransport.Connection connection) {
        }

        @Override
        public void onText(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull String data, int utf8Bytes) {
            this.text.set(data);
            this.utf8Bytes.set(utf8Bytes);
            this.textCallbacks.incrementAndGet();
        }

        @Override
        public void onPong(@NotNull RemoteWebSocketTransport.Connection connection) {
        }

        @Override
        public void onClose(@NotNull RemoteWebSocketTransport.Connection connection, int statusCode, @NotNull String reason) {
            this.closeStatus.set(statusCode);
            this.closeCallbacks.incrementAndGet();
        }

        @Override
        public void onError(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull Throwable error) {
        }
    }

    private static final class FakeWebSocket implements WebSocket {

        private String sentText;
        private byte[] sentPing;
        private int closeStatus = -1;
        private String closeReason;
        private boolean outputClosed;

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            this.sentText = data.toString();
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            this.sentPing = new byte[message.remaining()];
            message.get(this.sentPing);
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            this.closeStatus = statusCode;
            this.closeReason = reason;
            this.outputClosed = true;
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return this.outputClosed;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {
            this.outputClosed = true;
        }
    }
}
