package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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

class HttpInternetAvailabilityProbeTest {

    private static final URI ENDPOINT = URI.create("https://example.invalid/connectivity");

    @Test
    void configuresHeadTimeoutsAndDisconnectsAfterSuccessfulResponse() {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(204);
        HttpInternetAvailabilityProbe probe = probe(endpoint -> connection);

        assertTrue(probe.isAvailable());
        probe.close();

        assertEquals("HEAD", connection.getRequestMethod());
        assertEquals(3000, connection.getConnectTimeout());
        assertEquals(3000, connection.getReadTimeout());
        assertEquals(1, connection.disconnectCalls.get());
    }

    @Test
    void treatsNonSuccessfulResponseAsUnavailableAndDisconnects() {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(503);
        HttpInternetAvailabilityProbe probe = probe(endpoint -> connection);

        assertFalse(probe.isAvailable());

        assertEquals(1, connection.disconnectCalls.get());
    }

    @Test
    void treatsIoFailureAsUnavailableAndDisconnects() {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(new IOException("offline"));
        HttpInternetAvailabilityProbe probe = probe(endpoint -> connection);

        assertFalse(probe.isAvailable());

        assertEquals(1, connection.disconnectCalls.get());
    }

    @Test
    void disconnectsWhenUnexpectedFailureEscapes() {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(new IllegalStateException("broken connection"));
        HttpInternetAvailabilityProbe probe = probe(endpoint -> connection);

        assertThrows(IllegalStateException.class, probe::isAvailable);

        assertEquals(1, connection.disconnectCalls.get());
    }

    @Test
    void closeBeforeActiveAttemptPublicationStillDisconnectsTheNewConnectionExactlyOnce() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(204);
        CountDownLatch connectionOpened = new CountDownLatch(1);
        CountDownLatch publishConnection = new CountDownLatch(1);
        HttpInternetAvailabilityProbe.ConnectionFactory connectionFactory = endpoint -> {
            connectionOpened.countDown();
            await(publishConnection);
            return connection;
        };
        HttpInternetAvailabilityProbe probe = probe(connectionFactory);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> result = executor.submit(probe::isAvailable);
            assertTrue(connectionOpened.await(5L, TimeUnit.SECONDS));

            probe.close();
            publishConnection.countDown();

            assertFalse(result.get(5L, TimeUnit.SECONDS));
        } finally {
            publishConnection.countDown();
        }

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, connection.responseCalls.get());
    }

    @Test
    void concurrentCloseAndProbeFinallyDisconnectTheActiveAttemptExactlyOnce() throws Exception {
        CountDownLatch responseStarted = new CountDownLatch(1);
        CountDownLatch finishResponse = new CountDownLatch(1);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(204, responseStarted, finishResponse);
        connection.onDisconnect = finishResponse::countDown;
        HttpInternetAvailabilityProbe probe = probe(endpoint -> connection);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> result = executor.submit(probe::isAvailable);
            assertTrue(responseStarted.await(5L, TimeUnit.SECONDS));

            probe.close();
            probe.close();

            assertFalse(result.get(5L, TimeUnit.SECONDS));
        } finally {
            finishResponse.countDown();
        }

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(1, connection.responseCalls.get());
    }

    @Test
    void closedProbeDoesNotOpenAnotherConnection() {
        AtomicInteger openCalls = new AtomicInteger();
        HttpInternetAvailabilityProbe.ConnectionFactory connectionFactory = endpoint -> {
            openCalls.incrementAndGet();
            return new FakeHttpURLConnection(204);
        };
        HttpInternetAvailabilityProbe probe = probe(connectionFactory);

        probe.close();
        assertFalse(probe.isAvailable());

        assertEquals(0, openCalls.get());
    }

    private static HttpInternetAvailabilityProbe probe(HttpInternetAvailabilityProbe.ConnectionFactory connectionFactory) {
        return new HttpInternetAvailabilityProbe(ENDPOINT, 3000, 3000, connectionFactory);
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new IOException("Timed out waiting for test coordination");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for test coordination", ex);
        }
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {

        private final Object response;
        private final CountDownLatch responseStarted;
        private final CountDownLatch finishResponse;
        private final AtomicInteger responseCalls = new AtomicInteger();
        private final AtomicInteger disconnectCalls = new AtomicInteger();
        private Runnable onDisconnect = () -> {};

        private FakeHttpURLConnection(Object response) {
            this(response, null, null);
        }

        private FakeHttpURLConnection(Object response, CountDownLatch responseStarted, CountDownLatch finishResponse) {
            super(url());
            this.response = response;
            this.responseStarted = responseStarted;
            this.finishResponse = finishResponse;
        }

        @Override
        public int getResponseCode() throws IOException {
            this.responseCalls.incrementAndGet();
            if (this.responseStarted != null) this.responseStarted.countDown();
            if (this.finishResponse != null) await(this.finishResponse);
            if (this.response instanceof IOException exception) throw exception;
            if (this.response instanceof RuntimeException exception) throw exception;
            return (int) this.response;
        }

        @Override
        public void disconnect() {
            this.disconnectCalls.incrementAndGet();
            this.onDisconnect.run();
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {}

        private static URL url() {
            try {
                return ENDPOINT.toURL();
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }
    }
}
