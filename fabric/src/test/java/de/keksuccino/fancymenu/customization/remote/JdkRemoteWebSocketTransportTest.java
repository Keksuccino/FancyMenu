package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkRemoteWebSocketTransportTest {

    @Test
    void shutdownIsIdempotentTerminatesTheExecutorAndRejectsLateConnections() {
        JdkRemoteWebSocketTransport transport = new JdkRemoteWebSocketTransport(8, 2);
        CountingListener listener = new CountingListener();

        transport.shutdown();
        transport.shutdown();
        RemoteWebSocketTransport.Connection rejected = transport.connect(java.net.URI.create("ws://127.0.0.1:1/socket"), listener);

        assertTrue(transport.isTerminated());
        assertFalse(rejected.isOpen());
        assertInstanceOf(RejectedExecutionException.class, listener.lastError);
        assertEquals(0, listener.openCallbacks);
        assertEquals(1, listener.errorCallbacks);
    }

    @Test
    void fragmentedTextIsDeliveredAfterTheFinalFragmentWithOneAtATimeDemand() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 8, 4);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onText(socket, "1234", false);
        assertEquals(List.of(), listener.textEvents);
        connection.onText(socket, "5678", true);

        assertEquals(List.of(new TextEvent("12345678", 8)), listener.textEvents);
        assertEquals(3, socket.requests);
        assertEquals(1, listener.openCallbacks);
        assertTrue(connection.isOpen());
    }

    @Test
    void multibyteCodePointCanSpanTextFragmentsAtTheByteLimit() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 4, 2);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onText(socket, "\uD83D", false);
        connection.onText(socket, "\uDE00", true);

        assertEquals(List.of(new TextEvent("😀", 4)), listener.textEvents);
        assertEquals(3, socket.requests);
        assertEquals(0, listener.errorCallbacks);
    }

    @Test
    void fragmentedTextOverTheByteLimitClosesWithoutRequestingMoreInput() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 4, 3);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onText(socket, "123", false);
        connection.onText(socket, "45", true);

        assertEquals(1009, socket.closeStatus);
        assertEquals("inbound_message_too_large", socket.closeReason);
        assertEquals(2, socket.requests);
        assertEquals(List.of(), listener.textEvents);
        assertFalse(connection.isOpen());
        assertThrows(IllegalStateException.class, () -> connection.sendText("ignored"));
    }

    @Test
    void tooManyEmptyFragmentsCloseWithoutDeliveringText() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 8, 2);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onText(socket, "", false);
        connection.onText(socket, "", false);
        connection.onText(socket, "", true);

        assertEquals(1009, socket.closeStatus);
        assertEquals(3, socket.requests);
        assertEquals(List.of(), listener.textEvents);
    }

    @Test
    void fragmentedBinaryOverTheLimitClosesAndResetsInboundState() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 4, 2);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onBinary(socket, ByteBuffer.wrap(new byte[]{1, 2, 3}), false);
        connection.onBinary(socket, ByteBuffer.wrap(new byte[]{4, 5}), true);
        connection.onText(socket, "next", true);

        assertEquals(1009, socket.closeStatus);
        assertEquals(2, socket.requests);
        assertEquals(List.of(), listener.textEvents);
    }

    @Test
    void completedFragmentedBinaryResetsTheAggregateBeforeText() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 4, 2);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onBinary(socket, ByteBuffer.wrap(new byte[]{1, 2}), false);
        connection.onBinary(socket, ByteBuffer.wrap(new byte[]{3, 4}), true);
        connection.onText(socket, "next", true);

        assertEquals(-1, socket.closeStatus);
        assertEquals(4, socket.requests);
        assertEquals(List.of(new TextEvent("next", 4)), listener.textEvents);
    }

    @Test
    void malformedUtf16ClosesWithInvalidPayloadStatus() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 8, 2);
        FakeWebSocket socket = new FakeWebSocket();

        connection.onOpen(socket);
        connection.onText(socket, "\uD800", true);

        assertEquals(1007, socket.closeStatus);
        assertEquals("invalid_utf8_text_message", socket.closeReason);
        assertEquals(1, socket.requests);
        assertEquals(List.of(), listener.textEvents);
    }

    @Test
    void closeAndErrorCallbacksAreMutuallyTerminalAndDeliveredOnce() {
        CountingListener closeListener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection closedConnection = new JdkRemoteWebSocketTransport.JdkConnection(closeListener, 8, 2);
        FakeWebSocket closedSocket = new FakeWebSocket();
        closedConnection.onOpen(closedSocket);

        closedConnection.onClose(closedSocket, 1000, "done");
        closedConnection.onError(closedSocket, new IllegalStateException("late"));
        closedConnection.onClose(closedSocket, 1001, "duplicate");

        assertEquals(List.of(new CloseEvent(1000, "done")), closeListener.closeEvents);
        assertEquals(0, closeListener.errorCallbacks);
        assertFalse(closedConnection.isOpen());

        CountingListener errorListener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection failedConnection = new JdkRemoteWebSocketTransport.JdkConnection(errorListener, 8, 2);
        FakeWebSocket failedSocket = new FakeWebSocket();
        failedConnection.onOpen(failedSocket);

        failedConnection.onError(failedSocket, new IllegalStateException("failed"));
        failedConnection.onError(failedSocket, new IllegalStateException("duplicate"));
        failedConnection.onClose(failedSocket, 1000, "late");

        assertEquals(1, errorListener.errorCallbacks);
        assertEquals(List.of(), errorListener.closeEvents);
        assertFalse(failedConnection.isOpen());
    }

    @Test
    void outboundOperationsUseTheActiveWebSocketAndAbortIsIdempotent() {
        CountingListener listener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection connection = new JdkRemoteWebSocketTransport.JdkConnection(listener, 8, 2);
        FakeWebSocket socket = new FakeWebSocket();
        connection.onOpen(socket);

        connection.sendText("outbound").join();
        connection.sendPing(new byte[]{1, 2, 3}).join();
        connection.close(1000, "done").join();
        connection.close(1000, "duplicate").join();
        connection.abort();
        connection.abort();

        assertEquals(List.of("outbound"), socket.sentText);
        assertEquals(List.of(List.of((byte)1, (byte)2, (byte)3)), socket.sentPings);
        assertEquals(1000, socket.closeStatus);
        assertEquals("done", socket.closeReason);
        assertEquals(1, socket.abortCalls);
        assertFalse(connection.isOpen());
    }

    @Test
    void preOpenCloseAndAbortRejectLateSocketsWithoutTerminalCallbacks() {
        CountingListener closeListener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection closedConnection = new JdkRemoteWebSocketTransport.JdkConnection(closeListener, 8, 2);
        closedConnection.close(1000, "cancelled").join();
        FakeWebSocket closedSocket = new FakeWebSocket();
        closedConnection.onOpen(closedSocket);

        assertEquals(1, closedSocket.abortCalls);
        assertEquals(0, closeListener.openCallbacks);
        assertEquals(0, closeListener.errorCallbacks);
        assertEquals(List.of(), closeListener.closeEvents);

        CountingListener abortListener = new CountingListener();
        JdkRemoteWebSocketTransport.JdkConnection abortedConnection = new JdkRemoteWebSocketTransport.JdkConnection(abortListener, 8, 2);
        abortedConnection.abort();
        abortedConnection.abort();
        FakeWebSocket abortedSocket = new FakeWebSocket();
        abortedConnection.onOpen(abortedSocket);

        assertEquals(1, abortedSocket.abortCalls);
        assertEquals(0, abortListener.openCallbacks);
        assertEquals(0, abortListener.errorCallbacks);
        assertEquals(List.of(), abortListener.closeEvents);
    }

    private static final class CountingListener implements RemoteWebSocketTransport.Listener {

        private final List<TextEvent> textEvents = new ArrayList<>();
        private final List<CloseEvent> closeEvents = new ArrayList<>();
        private int openCallbacks;
        private int errorCallbacks;
        private Throwable lastError;

        @Override
        public void onOpen(@NotNull RemoteWebSocketTransport.Connection connection) {
            this.openCallbacks++;
        }

        @Override
        public void onText(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull String data, int utf8Bytes) {
            this.textEvents.add(new TextEvent(data, utf8Bytes));
        }

        @Override
        public void onPong(@NotNull RemoteWebSocketTransport.Connection connection) {
        }

        @Override
        public void onClose(@NotNull RemoteWebSocketTransport.Connection connection, int statusCode, @NotNull String reason) {
            this.closeEvents.add(new CloseEvent(statusCode, reason));
        }

        @Override
        public void onError(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull Throwable error) {
            this.errorCallbacks++;
            this.lastError = error;
        }
    }

    private static final class FakeWebSocket implements WebSocket {

        private final List<String> sentText = new ArrayList<>();
        private final List<List<Byte>> sentPings = new ArrayList<>();
        private int requests;
        private int closeStatus = -1;
        private String closeReason = "";
        private int abortCalls;

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            this.sentText.add(data.toString());
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            List<Byte> bytes = new ArrayList<>();
            while (message.hasRemaining()) bytes.add(message.get());
            this.sentPings.add(bytes);
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
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
            this.requests += Math.toIntExact(n);
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return this.closeStatus >= 0 || this.abortCalls > 0;
        }

        @Override
        public boolean isInputClosed() {
            return this.abortCalls > 0;
        }

        @Override
        public void abort() {
            this.abortCalls++;
        }
    }

    private record TextEvent(String text, int utf8Bytes) {
    }

    private record CloseEvent(int statusCode, String reason) {
    }
}
