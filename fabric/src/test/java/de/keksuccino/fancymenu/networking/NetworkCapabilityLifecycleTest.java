package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeProtocol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkCapabilityLifecycleTest {

    @Test
    void legacyCapabilityDoesNotImplyChunkProtocolSupport() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object connection = new Object();

        lifecycle.beginClientSession(connection);
        assertTrue(lifecycle.markClientServerCapable(connection, 0));

        assertTrue(lifecycle.isClientServerCapable(connection));
        assertFalse(lifecycle.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION));
    }

    @Test
    void advertisedProtocolIsBoundToTheExactClientSession() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        EqualIdentity advertisedConnection = new EqualIdentity();
        EqualIdentity replacementConnection = new EqualIdentity();

        lifecycle.beginClientSession(advertisedConnection);
        lifecycle.markClientServerCapable(advertisedConnection, BridgeProtocol.VERSION);
        assertTrue(lifecycle.supportsClientBridgeProtocol(advertisedConnection, BridgeProtocol.VERSION));

        lifecycle.beginClientSession(replacementConnection);
        assertFalse(lifecycle.supportsClientBridgeProtocol(advertisedConnection, BridgeProtocol.VERSION));
        assertFalse(lifecycle.supportsClientBridgeProtocol(replacementConnection, BridgeProtocol.VERSION));
    }

    @Test
    void serverConnectionsKeepIndependentAdvertisedVersions() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object legacyConnection = new Object();
        Object v1Connection = new Object();
        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, legacyConnection);
        lifecycle.beginServerConnection(server, v1Connection);

        lifecycle.markServerClientCapable(server, legacyConnection, 0);
        lifecycle.markServerClientCapable(server, v1Connection, BridgeProtocol.VERSION);

        assertTrue(lifecycle.isServerClientCapable(server, legacyConnection));
        assertFalse(lifecycle.supportsServerBridgeProtocol(server, legacyConnection, BridgeProtocol.VERSION));
        assertTrue(lifecycle.supportsServerBridgeProtocol(server, v1Connection, BridgeProtocol.VERSION));
    }

    @Test
    void futureAdvertisedVersionSupportsV1ButNotAnUnknownHigherMinimum() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object connection = new Object();
        lifecycle.beginClientSession(connection);
        lifecycle.markClientServerCapable(connection, BridgeProtocol.VERSION + 1);

        assertTrue(lifecycle.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION));
        assertTrue(lifecycle.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION + 1));
        assertFalse(lifecycle.supportsClientBridgeProtocol(connection, BridgeProtocol.VERSION + 2));
        assertThrows(IllegalArgumentException.class, () -> lifecycle.supportsClientBridgeProtocol(connection, 0));
    }

    @Test
    void clientSessionReplacementRejectsLateHandshakeAndDisconnect() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object oldConnection = new Object();
        Object replacementConnection = new Object();

        assertTrue(lifecycle.beginClientSession(oldConnection));
        assertTrue(lifecycle.markClientServerCapable(oldConnection));
        assertTrue(lifecycle.beginClientSession(replacementConnection));

        assertFalse(lifecycle.markClientServerCapable(oldConnection));
        assertFalse(lifecycle.endClientSession(oldConnection));
        assertTrue(lifecycle.isClientSessionActive(replacementConnection));
        assertFalse(lifecycle.isClientServerCapable(replacementConnection));
    }

    @Test
    void disconnectCleanupMakesReconnectWithoutHandshakeIncapable() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object firstConnection = new Object();
        Object reconnectedConnection = new Object();

        lifecycle.beginClientSession(firstConnection);
        lifecycle.markClientServerCapable(firstConnection);
        assertTrue(lifecycle.endClientSession(firstConnection));
        assertFalse(lifecycle.isClientServerCapable(firstConnection));

        assertTrue(lifecycle.beginClientSession(reconnectedConnection));
        assertTrue(lifecycle.isClientSessionActive(reconnectedConnection));
        assertFalse(lifecycle.isClientServerCapable(reconnectedConnection));
    }

    @Test
    void distinctEqualLocalWorldConnectionsDoNotShareCapability() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        EqualIdentity firstIntegratedWorld = new EqualIdentity();
        EqualIdentity secondIntegratedWorld = new EqualIdentity();

        lifecycle.beginClientSession(firstIntegratedWorld);
        lifecycle.markClientServerCapable(firstIntegratedWorld);
        lifecycle.beginClientSession(secondIntegratedWorld);

        assertFalse(lifecycle.isClientServerCapable(firstIntegratedWorld));
        assertFalse(lifecycle.isClientServerCapable(secondIntegratedWorld));
        assertTrue(lifecycle.markClientServerCapable(secondIntegratedWorld));
    }

    @Test
    void duplicateClientJoinAndHandshakeCallbacksAreIdempotent() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object connection = new Object();

        assertTrue(lifecycle.beginClientSession(connection));
        assertTrue(lifecycle.markClientServerCapable(connection));
        assertFalse(lifecycle.beginClientSession(connection));
        assertFalse(lifecycle.markClientServerCapable(connection));

        assertTrue(lifecycle.isClientServerCapable(connection));
    }

    @Test
    void nullOrDuplicateClientDisconnectCannotClearALiveSession() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object connection = new Object();

        lifecycle.beginClientSession(connection);
        lifecycle.markClientServerCapable(connection);

        assertFalse(lifecycle.endClientSession(null));
        assertTrue(lifecycle.isClientServerCapable(connection));
        assertTrue(lifecycle.endClientSession(connection));
        assertFalse(lifecycle.endClientSession(connection));
        assertFalse(lifecycle.isClientSessionActive(connection));
    }

    @Test
    void playerLogoutRemovesOnlyThatLiveConnection() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object firstPlayer = new Object();
        Object secondPlayer = new Object();

        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, firstPlayer);
        lifecycle.beginServerConnection(server, secondPlayer);
        lifecycle.markServerClientCapable(server, firstPlayer);
        lifecycle.markServerClientCapable(server, secondPlayer);

        assertTrue(lifecycle.endServerConnection(server, firstPlayer));
        assertFalse(lifecycle.isServerClientCapable(server, firstPlayer));
        assertTrue(lifecycle.isServerClientCapable(server, secondPlayer));
        assertEquals(1, lifecycle.liveServerConnectionCount(server));
        assertEquals(1, lifecycle.capableServerConnectionCount(server));
    }

    @Test
    void serverConnectionAdmissionRequiresStartedServerSession() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object connection = new Object();

        assertFalse(lifecycle.beginServerConnection(server, connection));
        assertFalse(lifecycle.markServerClientCapable(server, connection));
        assertTrue(lifecycle.beginServerSession(server));
        assertFalse(lifecycle.beginServerSession(server));
        assertTrue(lifecycle.beginServerConnection(server, connection));
        assertTrue(lifecycle.markServerClientCapable(server, connection));
    }

    @Test
    void serverStopCleanupIsIdempotentAndRejectsLateHandshake() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object playerConnection = new Object();

        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, playerConnection);
        lifecycle.markServerClientCapable(server, playerConnection);

        assertTrue(lifecycle.endServerSession(server));
        assertFalse(lifecycle.endServerSession(server));
        assertFalse(lifecycle.markServerClientCapable(server, playerConnection));
        assertFalse(lifecycle.isServerClientCapable(server, playerConnection));
        assertEquals(0, lifecycle.serverSessionCount());
    }

    @Test
    void restartedServerDoesNotInheritStoppedServerCapabilities() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        EqualIdentity stoppedServer = new EqualIdentity();
        EqualIdentity restartedServer = new EqualIdentity();
        EqualIdentity oldConnection = new EqualIdentity();
        EqualIdentity newConnection = new EqualIdentity();

        lifecycle.beginServerSession(stoppedServer);
        lifecycle.beginServerConnection(stoppedServer, oldConnection);
        lifecycle.markServerClientCapable(stoppedServer, oldConnection);
        lifecycle.endServerSession(stoppedServer);
        lifecycle.beginServerSession(restartedServer);
        lifecycle.beginServerConnection(restartedServer, newConnection);

        assertFalse(lifecycle.isServerClientCapable(restartedServer, oldConnection));
        assertFalse(lifecycle.isServerClientCapable(restartedServer, newConnection));
        assertTrue(lifecycle.markServerClientCapable(restartedServer, newConnection));
    }

    @Test
    void equalServersAndConnectionsRemainIndependentByIdentity() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        EqualIdentity firstServer = new EqualIdentity();
        EqualIdentity secondServer = new EqualIdentity();
        EqualIdentity firstConnection = new EqualIdentity();
        EqualIdentity secondConnection = new EqualIdentity();

        lifecycle.beginServerSession(firstServer);
        lifecycle.beginServerSession(secondServer);
        lifecycle.beginServerConnection(firstServer, firstConnection);
        lifecycle.beginServerConnection(secondServer, secondConnection);
        lifecycle.markServerClientCapable(firstServer, firstConnection);

        assertTrue(lifecycle.isServerClientCapable(firstServer, firstConnection));
        assertFalse(lifecycle.isServerClientCapable(firstServer, secondConnection));
        assertFalse(lifecycle.isServerClientCapable(secondServer, firstConnection));
        assertFalse(lifecycle.isServerClientCapable(secondServer, secondConnection));
        assertEquals(2, lifecycle.serverSessionCount());
    }

    @Test
    void multipleSimultaneousPlayersNegotiateIndependently() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        List<Object> connections = new ArrayList<>();

        lifecycle.beginServerSession(server);
        for (int index = 0; index < 32; index++) {
            Object connection = new Object();
            connections.add(connection);
            assertTrue(lifecycle.beginServerConnection(server, connection));
            if ((index & 1) == 0) assertTrue(lifecycle.markServerClientCapable(server, connection));
        }

        for (int index = 0; index < connections.size(); index++) assertEquals((index & 1) == 0, lifecycle.isServerClientCapable(server, connections.get(index)));
        assertEquals(32, lifecycle.liveServerConnectionCount(server));
        assertEquals(16, lifecycle.capableServerConnectionCount(server));
    }

    @Test
    void duplicateServerHandshakeIsIdempotent() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object connection = new Object();

        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, connection);

        assertTrue(lifecycle.markServerClientCapable(server, connection));
        assertFalse(lifecycle.markServerClientCapable(server, connection));
        assertEquals(1, lifecycle.capableServerConnectionCount(server));
    }

    @Test
    void handshakeCannotCreateCapabilityForUnknownOrLoggedOutConnection() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object connection = new Object();

        lifecycle.beginServerSession(server);
        assertFalse(lifecycle.markServerClientCapable(server, connection));
        lifecycle.beginServerConnection(server, connection);
        lifecycle.endServerConnection(server, connection);

        assertFalse(lifecycle.markServerClientCapable(server, connection));
        assertEquals(0, lifecycle.liveServerConnectionCount(server));
        assertEquals(0, lifecycle.capableServerConnectionCount(server));
    }

    @Test
    void concurrentDuplicateClientHandshakesHaveOneTransition() throws Exception {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object connection = new Object();
        AtomicInteger transitions = new AtomicInteger();
        lifecycle.beginClientSession(connection);

        runConcurrently(64, () -> {
            if (lifecycle.markClientServerCapable(connection)) transitions.incrementAndGet();
        });

        assertEquals(1, transitions.get());
        assertTrue(lifecycle.isClientServerCapable(connection));
    }

    @Test
    void concurrentLateClientCallbacksCannotMutateReplacementSession() throws Exception {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object oldConnection = new Object();
        Object replacementConnection = new Object();
        lifecycle.beginClientSession(oldConnection);
        lifecycle.markClientServerCapable(oldConnection);
        lifecycle.beginClientSession(replacementConnection);

        runConcurrently(64, () -> {
            lifecycle.markClientServerCapable(oldConnection);
            lifecycle.endClientSession(oldConnection);
        });

        assertTrue(lifecycle.isClientSessionActive(replacementConnection));
        assertFalse(lifecycle.isClientServerCapable(replacementConnection));
    }

    @Test
    void concurrentHandshakeAndLogoutLeaveNoStaleServerCapability() throws Exception {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object connection = new Object();
        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, connection);

        runConcurrently(64, () -> {
            lifecycle.markServerClientCapable(server, connection);
            lifecycle.endServerConnection(server, connection);
        });
        lifecycle.endServerConnection(server, connection);

        assertFalse(lifecycle.isServerClientCapable(server, connection));
        assertEquals(0, lifecycle.liveServerConnectionCount(server));
        assertEquals(0, lifecycle.capableServerConnectionCount(server));
    }

    private static void runConcurrently(int taskCount, ThrowingRunnable task) throws Exception {
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        try {
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    task.run();
                    return null;
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class EqualIdentity {

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualIdentity;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
