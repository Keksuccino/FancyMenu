package de.keksuccino.fancymenu.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedWebResourceClientTest {

    private static final URI ENDPOINT = URI.create("https://example.invalid/resource");

    @Test
    void endOfStreamClosesBodyCancelsDeadlineAndDisconnectsExactlyOnce() throws Exception {
        TrackingInputStream body = new TrackingInputStream("body".getBytes(StandardCharsets.UTF_8));
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));

        assertArrayEquals("body".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        assertEquals(-1, stream.read());
        stream.close();
        stream.close();

        assertEquals("GET", connection.getRequestMethod());
        assertEquals("Mozilla/4.0", connection.getRequestProperty("User-Agent"));
        assertTrue(connection.getInstanceFollowRedirects());
        assertEquals(500, connection.getConnectTimeout());
        assertEquals(1000, connection.getReadTimeout());
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(1, scheduler.tasks.getFirst().cancelCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void explicitCloseBeforeEndOfStreamReleasesEverythingExactlyOnce() throws Exception {
        TrackingInputStream body = new TrackingInputStream(new byte[]{1, 2, 3});
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));
        assertEquals(1, stream.read());
        stream.close();
        stream.close();

        assertThrows(IOException.class, stream::read);
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(1, scheduler.tasks.getFirst().cancelCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void bodyReadFailureClosesAndDisconnectsTheResponse() throws Exception {
        FailingInputStream body = new FailingInputStream();
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        BoundedWebResourceClient client = client(uri -> connection, new ManualDeadlineScheduler(), System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));
        IOException failure = assertThrows(IOException.class, stream::read);

        assertEquals("read failed", failure.getMessage());
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void rejectsDeclaredContentLengthBeforeOpeningTheBody() {
        TrackingInputStream body = new TrackingInputStream(new byte[0]);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        connection.contentLength = 6L;
        BoundedWebResourceClient client = client(uri -> connection, new ManualDeadlineScheduler(), System::nanoTime);

        IOException failure = assertThrows(IOException.class, () -> client.openResourceStream(ENDPOINT, limits(5L)));

        assertTrue(failure.getMessage().contains("5 byte limit"));
        assertEquals(0, connection.inputStreamCalls.get());
        assertEquals(0, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void rejectsUnknownLengthBodyAsSoonAsItCrossesTheStreamedLimit() throws Exception {
        TrackingInputStream body = new TrackingInputStream(new byte[]{1, 2, 3, 4, 5, 6});
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        connection.contentLength = -1L;
        BoundedWebResourceClient client = client(uri -> connection, new ManualDeadlineScheduler(), System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(5L));
        IOException failure = assertThrows(IOException.class, stream::readAllBytes);

        assertTrue(failure.getMessage().contains("5 byte limit"));
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void cumulativeDeadlineStopsAContinuouslyTricklingBody() throws Exception {
        AtomicLong nanoTime = new AtomicLong();
        AdvancingInputStream body = new AdvancingInputStream(new byte[]{1, 2, 3, 4}, nanoTime, Duration.ofSeconds(4L).toNanos());
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient.RequestLimits limits = new BoundedWebResourceClient.RequestLimits(Duration.ofSeconds(30L), Duration.ofSeconds(30L), Duration.ofSeconds(10L), 32L);
        BoundedWebResourceClient client = client(uri -> connection, scheduler, nanoTime::get);

        InputStream stream = client.openResourceStream(ENDPOINT, limits);
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertThrows(SocketTimeoutException.class, stream::read);

        assertEquals(List.of(10000, 10000, 6000, 2000), connection.readTimeouts);
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void watchdogExpiresAnOpenedBodyThatIsNeverRead() throws Exception {
        TrackingInputStream body = new TrackingInputStream(new byte[]{1});
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));
        scheduler.runTask(0);

        assertThrows(SocketTimeoutException.class, stream::read);
        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void watchdogUnblocksAndTerminatesAnActiveBodyRead() throws Exception {
        BlockingInputStream body = new BlockingInputStream(false);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        connection.onDisconnect = body::release;
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);
        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> read = executor.submit(() -> stream.read());
            assertTrue(body.operationStarted.await(5L, TimeUnit.SECONDS));
            scheduler.runTask(0);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> read.get(5L, TimeUnit.SECONDS));
            assertInstanceOf(SocketTimeoutException.class, failure.getCause());
        } finally {
            body.release();
        }

        assertEquals(1, body.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void availableCannotReturnAfterAConcurrentDeadline() throws Exception {
        BlockingInputStream body = new BlockingInputStream(true);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        connection.onDisconnect = body::release;
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);
        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> available = executor.submit(stream::available);
            assertTrue(body.operationStarted.await(5L, TimeUnit.SECONDS));
            scheduler.runTask(0);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> available.get(5L, TimeUnit.SECONDS));
            assertInstanceOf(SocketTimeoutException.class, failure.getCause());
        } finally {
            body.release();
        }

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void shutdownClosesActiveBodiesBeforeStoppingTheWatchdogAndRejectsLateRequests() throws Exception {
        List<String> lifecycle = new ArrayList<>();
        TrackingInputStream body = new TrackingInputStream(new byte[]{1}, () -> lifecycle.add("body-close"));
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, body);
        connection.onDisconnect = () -> lifecycle.add("disconnect");
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        scheduler.onShutdown = () -> lifecycle.add("scheduler-shutdown");
        AtomicInteger connectionCreations = new AtomicInteger();
        BoundedWebResourceClient client = client(uri -> {
            connectionCreations.incrementAndGet();
            return connection;
        }, scheduler, System::nanoTime);

        InputStream stream = client.openResourceStream(ENDPOINT, limits(32L));
        client.shutdown();
        client.shutdown();

        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, () -> client.openResourceStream(ENDPOINT, limits(32L)));
        assertEquals(List.of("disconnect", "body-close", "scheduler-shutdown"), lifecycle);
        assertEquals(1, scheduler.shutdownCalls.get());
        assertEquals(1, connectionCreations.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void shutdownRacingBetweenRegistrationAndDeadlineInstallationCannotLeakTheRequest() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, new TrackingInputStream(new byte[]{1}));
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        scheduler.blockSchedule = true;
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<InputStream> open = executor.submit(() -> client.openResourceStream(ENDPOINT, limits(32L)));
            assertTrue(scheduler.scheduleStarted.await(5L, TimeUnit.SECONDS));
            client.shutdown();
            scheduler.releaseSchedule.countDown();
            ExecutionException failure = assertThrows(ExecutionException.class, () -> open.get(5L, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, failure.getCause());
        } finally {
            scheduler.releaseSchedule.countDown();
        }

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, connection.responseCalls.get());
        assertEquals(1, scheduler.tasks.getFirst().cancelCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void schedulerRejectionAfterRegistrationDisconnectsTheRequest() {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, new TrackingInputStream(new byte[]{1}));
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        scheduler.scheduleFailure = new RejectedExecutionException("stopped");
        BoundedWebResourceClient client = client(uri -> connection, scheduler, System::nanoTime);

        assertThrows(IOException.class, () -> client.openResourceStream(ENDPOINT, limits(32L)));

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, connection.responseCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void shutdownRacingWithConnectionCreationDisconnectsBeforeRegistration() throws Exception {
        CountDownLatch connectionCreationStarted = new CountDownLatch(1);
        CountDownLatch releaseConnection = new CountDownLatch(1);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(200, new TrackingInputStream(new byte[]{1}));
        ManualDeadlineScheduler scheduler = new ManualDeadlineScheduler();
        BoundedWebResourceClient client = client(uri -> {
            connectionCreationStarted.countDown();
            await(releaseConnection);
            return connection;
        }, scheduler, System::nanoTime);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<InputStream> open = executor.submit(() -> client.openResourceStream(ENDPOINT, limits(32L)));
            assertTrue(connectionCreationStarted.await(5L, TimeUnit.SECONDS));
            client.shutdown();
            releaseConnection.countDown();
            ExecutionException failure = assertThrows(ExecutionException.class, () -> open.get(5L, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, failure.getCause());
        } finally {
            releaseConnection.countDown();
        }

        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, connection.responseCalls.get());
        assertTrue(scheduler.tasks.isEmpty());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void invalidStatusClosesItsErrorStreamAndDisconnects() {
        TrackingInputStream body = new TrackingInputStream(new byte[]{1});
        TrackingInputStream errorBody = new TrackingInputStream("missing".getBytes(StandardCharsets.UTF_8));
        FakeHttpURLConnection connection = new FakeHttpURLConnection(404, body);
        connection.errorStream = errorBody;
        BoundedWebResourceClient client = client(uri -> connection, new ManualDeadlineScheduler(), System::nanoTime);

        IOException failure = assertThrows(IOException.class, () -> client.openResourceStream(ENDPOINT, limits(32L)));

        assertTrue(failure.getMessage().contains("404"));
        assertEquals(0, connection.inputStreamCalls.get());
        assertEquals(1, errorBody.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void mimeProbeUsesGetPreservesHeaderValueAndDisconnectsOnAnyStatus() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(404, new TrackingInputStream(new byte[0]));
        TrackingInputStream errorBody = new TrackingInputStream(new byte[]{1});
        connection.errorStream = errorBody;
        connection.contentType = "text/plain; charset=UTF-8";
        BoundedWebResourceClient client = client(uri -> connection, new ManualDeadlineScheduler(), System::nanoTime);

        assertEquals("text/plain; charset=UTF-8", client.getMimeType(ENDPOINT, metadataLimits()));

        assertEquals("GET", connection.getRequestMethod());
        assertEquals(1, errorBody.closeCalls.get());
        assertEquals(1, connection.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void validityFallsBackToGetAfterHeadTransportFailureAndDisconnectsBothAttempts() throws Exception {
        FakeHttpURLConnection head = new FakeHttpURLConnection(new IOException("HEAD failed"), null);
        FakeHttpURLConnection get = new FakeHttpURLConnection(200, new TrackingInputStream(new byte[0]));
        BoundedWebResourceClient client = queuedClient(new ManualDeadlineScheduler(), head, get);

        assertTrue(client.isValidUrl(ENDPOINT, metadataLimits()));

        assertEquals("HEAD", head.getRequestMethod());
        assertEquals("GET", get.getRequestMethod());
        assertEquals(1, head.disconnectCalls.get());
        assertEquals(1, get.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {HttpURLConnection.HTTP_BAD_METHOD, HttpURLConnection.HTTP_NOT_IMPLEMENTED})
    void validityFallsBackToGetWhenHeadIsNotSupported(int headStatus) throws Exception {
        FakeHttpURLConnection head = new FakeHttpURLConnection(headStatus, null);
        TrackingInputStream headErrorBody = new TrackingInputStream(new byte[]{1});
        head.errorStream = headErrorBody;
        FakeHttpURLConnection get = new FakeHttpURLConnection(200, new TrackingInputStream(new byte[0]));
        BoundedWebResourceClient client = queuedClient(new ManualDeadlineScheduler(), head, get);

        assertTrue(client.isValidUrl(ENDPOINT, metadataLimits()));

        assertEquals("HEAD", head.getRequestMethod());
        assertEquals("GET", get.getRequestMethod());
        assertEquals(1, headErrorBody.closeCalls.get());
        assertEquals(1, head.disconnectCalls.get());
        assertEquals(1, get.disconnectCalls.get());
    }

    @Test
    void ordinaryHeadNotFoundDoesNotIssueGetFallback() throws Exception {
        AtomicInteger connectionCreations = new AtomicInteger();
        FakeHttpURLConnection head = new FakeHttpURLConnection(404, null);
        BoundedWebResourceClient client = client(uri -> {
            connectionCreations.incrementAndGet();
            return head;
        }, new ManualDeadlineScheduler(), System::nanoTime);

        assertFalse(client.isValidUrl(ENDPOINT, metadataLimits()));

        assertEquals(1, connectionCreations.get());
        assertEquals(1, head.disconnectCalls.get());
    }

    @Test
    void failedHeadAndGetFallbackDisconnectBothConnections() {
        FakeHttpURLConnection head = new FakeHttpURLConnection(new IOException("HEAD failed"), null);
        FakeHttpURLConnection get = new FakeHttpURLConnection(new IOException("GET failed"), null);
        BoundedWebResourceClient client = queuedClient(new ManualDeadlineScheduler(), head, get);

        assertThrows(IOException.class, () -> client.isValidUrl(ENDPOINT, metadataLimits()));

        assertEquals(1, head.disconnectCalls.get());
        assertEquals(1, get.disconnectCalls.get());
        assertEquals(0, client.activeRequestCount());
    }

    @Test
    void realHttpRedirectPreservesMimeAndBodyBehavior() throws Exception {
        AtomicInteger assetRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/asset");
            exchange.sendResponseHeaders(302, -1L);
            exchange.close();
        });
        server.createContext("/asset", exchange -> {
            assetRequests.incrementAndGet();
            byte[] body = "asset-body".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            URI redirect = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/redirect");
            BoundedWebResourceClient client = client(uri -> (HttpURLConnection)uri.toURL().openConnection(), new ManualDeadlineScheduler(), System::nanoTime);

            assertEquals("image/png", client.getMimeType(redirect, metadataLimits()));
            try (InputStream stream = client.openResourceStream(redirect, limits(64L))) {
                assertEquals("asset-body", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }

            assertEquals(2, assetRequests.get());
            assertEquals(0, client.activeRequestCount());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void realSocketReadTimeoutTerminatesAStalledBodyWithoutExternalNetwork() throws Exception {
        CountDownLatch responseStarted = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stall", exchange -> holdResponseOpen(exchange, responseStarted, releaseResponse));
        server.start();

        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/stall");
            BoundedWebResourceClient.RequestLimits limits = new BoundedWebResourceClient.RequestLimits(Duration.ofSeconds(1L), Duration.ofMillis(150L), Duration.ofSeconds(5L), 32L);
            BoundedWebResourceClient client = client(uri -> (HttpURLConnection)uri.toURL().openConnection(), new ManualDeadlineScheduler(), System::nanoTime);
            InputStream stream = client.openResourceStream(endpoint, limits);
            assertTrue(responseStarted.await(5L, TimeUnit.SECONDS));
            assertEquals(1, stream.read());

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<Integer> read = executor.submit(() -> stream.read());
                ExecutionException failure = assertThrows(ExecutionException.class, () -> read.get(5L, TimeUnit.SECONDS));
                assertInstanceOf(SocketTimeoutException.class, failure.getCause());
            }

            assertEquals(0, client.activeRequestCount());
        } finally {
            releaseResponse.countDown();
            server.stop(0);
        }
    }

    private static void holdResponseOpen(HttpExchange exchange, CountDownLatch responseStarted, CountDownLatch releaseResponse) throws IOException {
        try {
            exchange.sendResponseHeaders(200, 0L);
            exchange.getResponseBody().write(1);
            exchange.getResponseBody().flush();
            responseStarted.countDown();
            if (!releaseResponse.await(5L, TimeUnit.SECONDS)) throw new IOException("Timed out waiting to release loopback response");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while holding loopback response", exception);
        } finally {
            exchange.close();
        }
    }

    private static BoundedWebResourceClient client(BoundedWebResourceClient.ConnectionFactory connectionFactory, DeadlineScheduler scheduler, LongSupplier nanoTimeSource) {
        return new BoundedWebResourceClient(connectionFactory, scheduler, nanoTimeSource);
    }

    private static BoundedWebResourceClient queuedClient(DeadlineScheduler scheduler, FakeHttpURLConnection... connections) {
        Deque<FakeHttpURLConnection> queuedConnections = new ArrayDeque<>(List.of(connections));
        return client(uri -> queuedConnections.removeFirst(), scheduler, System::nanoTime);
    }

    private static BoundedWebResourceClient.RequestLimits limits(long maximumBytes) {
        return new BoundedWebResourceClient.RequestLimits(Duration.ofMillis(500L), Duration.ofSeconds(1L), Duration.ofSeconds(2L), maximumBytes);
    }

    private static BoundedWebResourceClient.RequestLimits metadataLimits() {
        return new BoundedWebResourceClient.RequestLimits(Duration.ofSeconds(1L), Duration.ofSeconds(1L), Duration.ofSeconds(3L), 1L);
    }

    private static final class ManualDeadlineScheduler implements DeadlineScheduler {

        private final List<ManualScheduledTask> tasks = new ArrayList<>();
        private final AtomicInteger shutdownCalls = new AtomicInteger();
        private final CountDownLatch scheduleStarted = new CountDownLatch(1);
        private final CountDownLatch releaseSchedule = new CountDownLatch(1);
        private RuntimeException scheduleFailure;
        private Runnable onShutdown = () -> {};
        private boolean blockSchedule;

        @Override
        public ScheduledTask schedule(Runnable task, Duration delay) {
            if (this.scheduleFailure != null) throw this.scheduleFailure;
            if (delay.isNegative() || delay.isZero()) throw new IllegalArgumentException("delay must be positive");
            if (this.blockSchedule) {
                this.scheduleStarted.countDown();
                await(this.releaseSchedule);
            }
            ManualScheduledTask scheduledTask = new ManualScheduledTask(task);
            this.tasks.add(scheduledTask);
            return scheduledTask;
        }

        @Override
        public void shutdownNow() {
            this.shutdownCalls.incrementAndGet();
            this.onShutdown.run();
        }

        private void runTask(int index) {
            this.tasks.get(index).run();
        }
    }

    private static final class ManualScheduledTask implements DeadlineScheduler.ScheduledTask {

        private final Runnable command;
        private final AtomicInteger cancelCalls = new AtomicInteger();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private ManualScheduledTask(Runnable command) {
            this.command = command;
        }

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            this.cancelCalls.incrementAndGet();
            this.cancelled.set(true);
        }

        private void run() {
            if (this.cancelled.get()) throw new IllegalStateException("Scheduled task is cancelled");
            this.command.run();
        }
    }

    private static class FakeHttpURLConnection extends HttpURLConnection {

        private final Object response;
        private final InputStream inputStream;
        private final AtomicInteger responseCalls = new AtomicInteger();
        private final AtomicInteger inputStreamCalls = new AtomicInteger();
        private final AtomicInteger disconnectCalls = new AtomicInteger();
        private final List<Integer> readTimeouts = new ArrayList<>();
        private long contentLength = -1L;
        private String contentType;
        private InputStream errorStream;
        private Runnable onDisconnect = () -> {};

        private FakeHttpURLConnection(Object response, InputStream inputStream) {
            super(url());
            this.response = response;
            this.inputStream = inputStream;
        }

        @Override
        public int getResponseCode() throws IOException {
            this.responseCalls.incrementAndGet();
            if (this.response instanceof IOException exception) throw exception;
            if (this.response instanceof RuntimeException exception) throw exception;
            return (int)this.response;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            this.inputStreamCalls.incrementAndGet();
            if (this.inputStream == null) throw new IOException("No response body");
            return this.inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return this.errorStream;
        }

        @Override
        public long getContentLengthLong() {
            return this.contentLength;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public void setReadTimeout(int timeout) {
            super.setReadTimeout(timeout);
            this.readTimeouts.add(timeout);
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
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static class TrackingInputStream extends ByteArrayInputStream {

        protected final AtomicInteger closeCalls = new AtomicInteger();
        private final Runnable onClose;

        private TrackingInputStream(byte[] bytes) {
            this(bytes, () -> {});
        }

        private TrackingInputStream(byte[] bytes, Runnable onClose) {
            super(bytes);
            this.onClose = onClose;
        }

        @Override
        public void close() throws IOException {
            this.closeCalls.incrementAndGet();
            this.onClose.run();
            super.close();
        }
    }

    private static final class AdvancingInputStream extends TrackingInputStream {

        private final AtomicLong nanoTime;
        private final long advanceNanos;

        private AdvancingInputStream(byte[] bytes, AtomicLong nanoTime, long advanceNanos) {
            super(bytes);
            this.nanoTime = nanoTime;
            this.advanceNanos = advanceNanos;
        }

        @Override
        public synchronized int read() {
            this.nanoTime.addAndGet(this.advanceNanos);
            return super.read();
        }
    }

    private static final class FailingInputStream extends InputStream {

        private final AtomicInteger closeCalls = new AtomicInteger();

        @Override
        public int read() throws IOException {
            throw new IOException("read failed");
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
        }
    }

    private static final class BlockingInputStream extends InputStream {

        private final CountDownLatch operationStarted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger closeCalls = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final boolean blockAvailable;

        private BlockingInputStream(boolean blockAvailable) {
            this.blockAvailable = blockAvailable;
        }

        @Override
        public int read() throws IOException {
            this.awaitRelease();
            if (this.closed.get()) throw new IOException("stream closed");
            return 1;
        }

        @Override
        public int available() throws IOException {
            if (!this.blockAvailable) return 0;
            this.awaitRelease();
            if (this.closed.get()) throw new IOException("stream closed");
            return 1;
        }

        @Override
        public void close() {
            this.closeCalls.incrementAndGet();
            this.closed.set(true);
            this.release();
        }

        private void awaitRelease() throws IOException {
            this.operationStarted.countDown();
            try {
                if (!this.release.await(5L, TimeUnit.SECONDS)) throw new IOException("Timed out waiting for test release");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for test release", exception);
            }
        }

        private void release() {
            this.release.countDown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test coordination");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for test coordination", exception);
        }
    }
}
