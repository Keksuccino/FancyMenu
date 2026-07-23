package de.keksuccino.fancymenu.customization.remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void rejectsOversizedPayloadBeforeAdmittingConnectionState() {
        int envelopeBytes = "request_id=".length() + 64 + 1;
        String oversizedPayload = "a".repeat(RemoteServerConnectionManager.MAX_OUTBOUND_MESSAGE_UTF8_BYTES - envelopeBytes + 1);

        RemoteServerConnectionManager.sendData("ws://127.0.0.1:1/test", oversizedPayload);

        assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
    }

    @Test
    void rejectsMalformedUtf16BeforeAdmittingConnectionState() {
        RemoteServerConnectionManager.sendData("ws://127.0.0.1:1/test", "\uD83D");

        assertEquals(0, RemoteServerConnectionManager.activeConnectionStateCount());
    }

    @Test
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
