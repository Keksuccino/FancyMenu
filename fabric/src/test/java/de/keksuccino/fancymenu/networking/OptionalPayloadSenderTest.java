package de.keksuccino.fancymenu.networking;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalPayloadSenderTest {

    @Test
    void supportedClientboundPayloadSendsExactlyOnce() {
        Endpoint serverListener = new Endpoint(Direction.CLIENTBOUND);
        Object payload = new Object();
        AtomicInteger predicateCalls = new AtomicInteger();
        AtomicInteger sendCalls = new AtomicInteger();

        boolean sent = OptionalPayloadSender.sendIfSupported(serverListener, payload, (endpoint, candidate) -> {
            predicateCalls.incrementAndGet();
            assertSame(serverListener, endpoint);
            assertSame(payload, candidate);
            return endpoint.direction == Direction.CLIENTBOUND;
        }, (endpoint, candidate) -> {
            sendCalls.incrementAndGet();
            assertSame(serverListener, endpoint);
            assertSame(payload, candidate);
        });

        assertTrue(sent);
        assertEquals(1, predicateCalls.get());
        assertEquals(1, sendCalls.get());
    }

    @Test
    void supportedServerboundPayloadSendsExactlyOnce() {
        Endpoint clientConnection = new Endpoint(Direction.SERVERBOUND);
        Object payload = new Object();
        AtomicInteger sendCalls = new AtomicInteger();

        assertTrue(OptionalPayloadSender.sendIfSupported(clientConnection, payload, (endpoint, candidate) -> endpoint.direction == Direction.SERVERBOUND, (endpoint, candidate) -> sendCalls.incrementAndGet()));
        assertEquals(1, sendCalls.get());
    }

    @Test
    void unsupportedPayloadIsANormalNoOp() {
        Endpoint connection = new Endpoint(Direction.SERVERBOUND);
        AtomicInteger predicateCalls = new AtomicInteger();
        AtomicInteger sendCalls = new AtomicInteger();

        assertFalse(OptionalPayloadSender.sendIfSupported(connection, new Object(), (endpoint, payload) -> {
            predicateCalls.incrementAndGet();
            return false;
        }, (endpoint, payload) -> sendCalls.incrementAndGet()));
        assertEquals(1, predicateCalls.get());
        assertEquals(0, sendCalls.get());
    }

    @Test
    void supportIsCheckedAgainstTheExactEndpointIdentity() {
        Endpoint negotiatedConnection = new Endpoint(Direction.SERVERBOUND);
        Endpoint equalReplacementConnection = new Endpoint(Direction.SERVERBOUND);
        AtomicReference<Endpoint> checkedConnection = new AtomicReference<>();
        AtomicInteger sendCalls = new AtomicInteger();

        assertEquals(negotiatedConnection, equalReplacementConnection);
        assertFalse(OptionalPayloadSender.sendIfSupported(equalReplacementConnection, new Object(), (endpoint, payload) -> {
            checkedConnection.set(endpoint);
            return endpoint == negotiatedConnection;
        }, (endpoint, payload) -> sendCalls.incrementAndGet()));
        assertSame(equalReplacementConnection, checkedConnection.get());
        assertEquals(0, sendCalls.get());

        assertTrue(OptionalPayloadSender.sendIfSupported(negotiatedConnection, new Object(), (endpoint, payload) -> {
            checkedConnection.set(endpoint);
            return endpoint == negotiatedConnection;
        }, (endpoint, payload) -> sendCalls.incrementAndGet()));
        assertSame(negotiatedConnection, checkedConnection.get());
        assertEquals(1, sendCalls.get());
    }

    @Test
    void nullInputsAreRejectedBeforeSupportEvaluation() {
        Endpoint endpoint = new Endpoint(Direction.SERVERBOUND);
        Object payload = new Object();
        AtomicInteger predicateCalls = new AtomicInteger();

        assertThrows(NullPointerException.class, () -> OptionalPayloadSender.sendIfSupported(null, payload, (candidate, value) -> {
            predicateCalls.incrementAndGet();
            return true;
        }, (candidate, value) -> {}));
        assertThrows(NullPointerException.class, () -> OptionalPayloadSender.sendIfSupported(endpoint, null, (candidate, value) -> {
            predicateCalls.incrementAndGet();
            return true;
        }, (candidate, value) -> {}));
        assertThrows(NullPointerException.class, () -> OptionalPayloadSender.sendIfSupported(endpoint, payload, null, (candidate, value) -> {}));
        assertThrows(NullPointerException.class, () -> OptionalPayloadSender.sendIfSupported(endpoint, payload, (candidate, value) -> {
            predicateCalls.incrementAndGet();
            return false;
        }, null));
        assertEquals(0, predicateCalls.get());
    }

    @Test
    void supportPredicateFailurePropagatesWithoutSending() {
        RuntimeException failure = new RuntimeException("support check failed");
        AtomicInteger sendCalls = new AtomicInteger();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> OptionalPayloadSender.sendIfSupported(new Endpoint(Direction.CLIENTBOUND), new Object(), (endpoint, payload) -> {
            throw failure;
        }, (endpoint, payload) -> sendCalls.incrementAndGet()));

        assertSame(failure, thrown);
        assertEquals(0, sendCalls.get());
    }

    @Test
    void negotiatedSendFailurePropagates() {
        RuntimeException failure = new RuntimeException("send failed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> OptionalPayloadSender.sendIfSupported(new Endpoint(Direction.CLIENTBOUND), new Object(), (endpoint, payload) -> true, (endpoint, payload) -> {
            throw failure;
        }));

        assertSame(failure, thrown);
    }

    @Test
    void skippedClientboundHandshakeCannotMarkServerCapability() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object clientConnection = new Object();
        Endpoint serverListener = new Endpoint(Direction.CLIENTBOUND);
        AtomicInteger receiveCalls = new AtomicInteger();
        lifecycle.beginClientSession(clientConnection);

        assertFalse(OptionalPayloadSender.sendIfSupported(serverListener, new Object(), (endpoint, payload) -> false, (endpoint, payload) -> {
            receiveCalls.incrementAndGet();
            lifecycle.markClientServerCapable(clientConnection);
        }));
        assertEquals(0, receiveCalls.get());
        assertFalse(lifecycle.isClientServerCapable(clientConnection));
    }

    @Test
    void skippedServerboundHandshakeCannotMarkClientCapability() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object server = new Object();
        Object serverConnection = new Object();
        Endpoint clientConnection = new Endpoint(Direction.SERVERBOUND);
        AtomicInteger receiveCalls = new AtomicInteger();
        lifecycle.beginServerSession(server);
        lifecycle.beginServerConnection(server, serverConnection);

        assertFalse(OptionalPayloadSender.sendIfSupported(clientConnection, new Object(), (endpoint, payload) -> false, (endpoint, payload) -> {
            receiveCalls.incrementAndGet();
            lifecycle.markServerClientCapable(server, serverConnection);
        }));
        assertEquals(0, receiveCalls.get());
        assertFalse(lifecycle.isServerClientCapable(server, serverConnection));
    }

    @Test
    void duplicateSupportedHandshakesSendNormallyButCapabilityTransitionsOnce() {
        NetworkCapabilityLifecycle lifecycle = new NetworkCapabilityLifecycle();
        Object clientConnection = new Object();
        Endpoint serverListener = new Endpoint(Direction.CLIENTBOUND);
        AtomicInteger sendCalls = new AtomicInteger();
        AtomicInteger capabilityTransitions = new AtomicInteger();
        lifecycle.beginClientSession(clientConnection);

        for (int attempt = 0; attempt < 2; attempt++) {
            assertTrue(OptionalPayloadSender.sendIfSupported(serverListener, new Object(), (endpoint, payload) -> true, (endpoint, payload) -> {
                sendCalls.incrementAndGet();
                if (lifecycle.markClientServerCapable(clientConnection)) capabilityTransitions.incrementAndGet();
            }));
        }

        assertEquals(2, sendCalls.get());
        assertEquals(1, capabilityTransitions.get());
        assertTrue(lifecycle.isClientServerCapable(clientConnection));
    }

    private enum Direction {
        CLIENTBOUND,
        SERVERBOUND
    }

    private static final class Endpoint {

        private final Direction direction;

        private Endpoint(Direction direction) {
            this.direction = direction;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Endpoint endpoint && this.direction == endpoint.direction;
        }

        @Override
        public int hashCode() {
            return this.direction.hashCode();
        }
    }
}
