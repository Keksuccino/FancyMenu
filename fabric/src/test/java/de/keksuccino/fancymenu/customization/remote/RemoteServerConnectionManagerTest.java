package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manager shutdown is intentionally permanent. The terminal test is ordered last and this class is forced onto one
 * thread so it cannot contaminate its earlier singleton tests even if JUnit parallel execution is enabled later.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
class RemoteServerConnectionManagerTest {

    @BeforeEach
    void clearConnections() {
        RemoteServerConnectionManager.closeAllConnections();
        assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
    }

    @AfterEach
    void cleanupConnections() {
        RemoteServerConnectionManager.closeAllConnections();
    }

    @Test
    @Order(1)
    void rejectsOversizedPayloadBeforeAdmittingConnectionState() {
        int envelopeBytes = "request_id=".length() + 64 + 1;
        String oversizedPayload = "a".repeat(RemoteServerConnectionManager.MAX_OUTBOUND_MESSAGE_UTF8_BYTES - envelopeBytes + 1);

        RemoteServerConnectionManager.sendData("ws://127.0.0.1:1/test", oversizedPayload);

        assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
    }

    @Test
    @Order(2)
    void rejectsMalformedUtf16BeforeAdmittingConnectionState() {
        RemoteServerConnectionManager.sendData("ws://127.0.0.1:1/test", "\uD83D");

        assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
    }

    @Test
    @Order(3)
    void staleGenerationCallbacksCannotReplaceOrClearTheCurrentConnection() {
        RemoteServerConnectionManager.ConnectionState state = new RemoteServerConnectionManager.ConnectionState("ws://test", "request");
        RemoteServerConnectionManager.TransportListener firstGeneration = new RemoteServerConnectionManager.TransportListener(state, 0L);
        FakeConnection firstConnection = new FakeConnection();
        firstGeneration.onOpen(firstConnection);
        assertSame(firstConnection, state.currentConnection());

        firstGeneration.onClose(firstConnection, 1000, "complete");
        assertNull(state.currentConnection());
        assertEquals(1L, state.currentGeneration());

        RemoteServerConnectionManager.TransportListener secondGeneration = new RemoteServerConnectionManager.TransportListener(state, 1L);
        FakeConnection secondConnection = new FakeConnection();
        secondGeneration.onOpen(secondConnection);
        assertSame(secondConnection, state.currentConnection());

        firstGeneration.onClose(firstConnection, 1006, "stale_close");
        firstGeneration.onError(firstConnection, new IllegalStateException("stale_error"));
        firstGeneration.onPong(firstConnection);
        firstGeneration.onOpen(firstConnection);

        assertSame(secondConnection, state.currentConnection());
        assertEquals(1L, state.currentGeneration());
        assertTrue(firstConnection.aborted);
    }

    @Test
    @Order(Integer.MAX_VALUE)
    void shutdownIsPermanentIdempotentAndQuiescesAllOwnedState() throws Exception {
        try (ServerSocket handshakeStall = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            String remoteServerUrl = "ws://127.0.0.1:" + handshakeStall.getLocalPort() + "/socket";
            RemoteServerConnectionManager.sendData(remoteServerUrl, "queued-before-shutdown");
            RemoteServerConnectionManager.ConnectionState state = RemoteServerConnectionManager.activeConnectionState(remoteServerUrl);
            assertNotNull(state);
            long preShutdownGeneration = state.currentGeneration();
            assertTrue(state.isRegistered());
            assertTrue(state.isReconnectRequested());
            assertNotNull(state.currentConnection());
            assertEquals(1, state.pendingOutboundPayloadCount());
            assertEquals(1, RemoteServerConnectionManager.activeConnectionStateCount());
            assertEquals(1, RemoteServerConnectionManager.cachedRequestIdCount());

            int racingSends = 32;
            CountDownLatch ready = new CountDownLatch(racingSends);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> sends = new ArrayList<>();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int index = 0; index < racingSends; index++) {
                    sends.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        RemoteServerConnectionManager.sendData(remoteServerUrl, "racing-shutdown");
                        return null;
                    }));
                }
                try {
                    assertTrue(ready.await(5, TimeUnit.SECONDS));
                    start.countDown();
                    Thread.currentThread().interrupt();
                    try {
                        RemoteServerConnectionManager.shutdown();
                        assertTrue(Thread.currentThread().isInterrupted());
                    } finally {
                        Thread.interrupted();
                    }
                    for (Future<?> send : sends) {
                        send.get(5, TimeUnit.SECONDS);
                    }
                } finally {
                    start.countDown();
                }
            }

            assertTrue(RemoteServerConnectionManager.isShutdownStarted());
            assertTrue(RemoteServerConnectionManager.areWorkersTerminated());
            assertFalse(state.isRegistered());
            assertFalse(state.isReconnectRequested());
            assertNull(state.currentConnection());
            assertEquals(preShutdownGeneration + 1L, state.currentGeneration());
            assertEquals(0, state.pendingOutboundPayloadCount());
            assertEquals(0, state.pendingInboundDeliveryCount());
            assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
            assertEquals(0, RemoteServerConnectionManager.cachedRequestIdCount());

            FakeConnection lateConnection = new FakeConnection();
            new RemoteServerConnectionManager.TransportListener(state, state.currentGeneration()).onOpen(lateConnection);
            assertTrue(lateConnection.aborted);
            assertNull(state.currentConnection());

            RemoteServerConnectionManager.shutdown();
            RemoteServerConnectionManager.connect(remoteServerUrl);
            RemoteServerConnectionManager.sendData(remoteServerUrl, "rejected-after-shutdown");
            assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
            assertEquals(0, RemoteServerConnectionManager.cachedRequestIdCount());
            assertTrue(RemoteServerConnectionManager.areWorkersTerminated());
        }
    }

    private static final class FakeConnection implements RemoteWebSocketTransport.Connection {

        private boolean open = true;
        private boolean aborted;

        @Override
        public boolean isOpen() {
            return this.open;
        }

        @Override
        public CompletableFuture<Void> sendText(String data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendPing(byte[] data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> close(int statusCode, String reason) {
            this.open = false;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void abort() {
            this.open = false;
            this.aborted = true;
        }
    }
}
