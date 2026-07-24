package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Owns every HTTP connection used by the shared URL helpers. A request remains registered until its response body
 * reaches EOF or is explicitly closed, which lets both the overall-deadline watchdog and client shutdown release an
 * abandoned stream even when its original caller no longer has a reference to the connection.
 */
final class BoundedWebResourceClient {

    private static final String USER_AGENT = "Mozilla/4.0";

    private final Object lifecycleLock = new Object();
    private final Set<ActiveRequest> activeRequests = new HashSet<>();
    private final ConnectionFactory connectionFactory;
    private final DeadlineScheduler deadlineScheduler;
    private final LongSupplier nanoTimeSource;
    private boolean closed;

    BoundedWebResourceClient(@NotNull ConnectionFactory connectionFactory, @NotNull DeadlineScheduler deadlineScheduler, @NotNull LongSupplier nanoTimeSource) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.deadlineScheduler = Objects.requireNonNull(deadlineScheduler, "deadlineScheduler");
        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
    }

    @NotNull
    InputStream openResourceStream(@NotNull URI resourceUri, @NotNull RequestLimits limits) throws IOException {
        Objects.requireNonNull(resourceUri, "resourceUri");
        RequestLimits checkedLimits = Objects.requireNonNull(limits, "limits");
        RequestDeadline deadline = new RequestDeadline(checkedLimits.overallTimeout(), this.nanoTimeSource);
        ActiveRequest request = this.startRequest(resourceUri, "GET", checkedLimits, deadline);
        try {
            int responseCode = this.getResponseCode(request, deadline);
            if ((responseCode < 200) || (responseCode >= 300)) {
                InputStream errorStream = request.connection().getErrorStream();
                if (errorStream != null) request.attachResponseStream(errorStream);
                throw new IOException("Web resource returned HTTP status " + responseCode);
            }
            long contentLength = request.connection().getContentLengthLong();
            if (contentLength > checkedLimits.maximumBytes()) throw new ResourceSizeLimitException(checkedLimits.maximumBytes(), contentLength);
            InputStream responseStream = request.connection().getInputStream();
            request.attachResponseStream(responseStream);
            this.ensureWithinDeadline(request, deadline);
            return new BoundedResponseInputStream(responseStream, request, deadline, checkedLimits);
        } catch (ResourceSizeLimitException exception) {
            addCleanupFailure(exception, request.terminate(Termination.SIZE_LIMIT_EXCEEDED));
            throw exception;
        } catch (IOException exception) {
            addCleanupFailure(exception, request.terminate(Termination.FAILED));
            throw request.mapIOException(exception);
        } catch (RuntimeException | Error failure) {
            addCleanupFailure(failure, request.terminate(Termination.FAILED));
            throw failure;
        }
    }

    @Nullable
    String getMimeType(@NotNull URI resourceUri, @NotNull RequestLimits limits) throws IOException {
        ResponseMetadata response = this.requestMetadata(resourceUri, "GET", limits, new RequestDeadline(limits.overallTimeout(), this.nanoTimeSource));
        return response.mimeType();
    }

    boolean isValidUrl(@NotNull URI resourceUri, @NotNull RequestLimits limits) throws IOException {
        RequestDeadline deadline = new RequestDeadline(limits.overallTimeout(), this.nanoTimeSource);
        ResponseMetadata headResponse;
        try {
            headResponse = this.requestMetadata(resourceUri, "HEAD", limits, deadline);
        } catch (IOException headFailure) {
            return this.getFallbackValidity(resourceUri, limits, deadline, headFailure);
        }
        if (headResponse.responseCode() == HttpURLConnection.HTTP_OK) return true;
        if ((headResponse.responseCode() != HttpURLConnection.HTTP_BAD_METHOD) && (headResponse.responseCode() != HttpURLConnection.HTTP_NOT_IMPLEMENTED)) return false;
        return this.getFallbackValidity(resourceUri, limits, deadline, null);
    }

    void shutdown() {
        List<ActiveRequest> requestsToClose;
        synchronized (this.lifecycleLock) {
            if (this.closed) return;
            this.closed = true;
            requestsToClose = new ArrayList<>(this.activeRequests);
        }
        // Active connections must be closed before the watchdog stops; otherwise cancelling its queued tasks could
        // remove the only remaining release path for a response body abandoned by its original loader.
        for (ActiveRequest request : requestsToClose) request.terminate(Termination.CLIENT_SHUTDOWN);
        this.deadlineScheduler.shutdownNow();
    }

    int activeRequestCount() {
        synchronized (this.lifecycleLock) {
            return this.activeRequests.size();
        }
    }

    private boolean getFallbackValidity(@NotNull URI resourceUri, @NotNull RequestLimits limits, @NotNull RequestDeadline deadline, @Nullable IOException headFailure) throws IOException {
        try {
            return this.requestMetadata(resourceUri, "GET", limits, deadline).responseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException getFailure) {
            if (headFailure != null) getFailure.addSuppressed(headFailure);
            throw getFailure;
        }
    }

    @NotNull
    private ResponseMetadata requestMetadata(@NotNull URI resourceUri, @NotNull String method, @NotNull RequestLimits limits, @NotNull RequestDeadline deadline) throws IOException {
        ActiveRequest request = this.startRequest(resourceUri, method, limits, deadline);
        try {
            int responseCode = this.getResponseCode(request, deadline);
            String mimeType = request.connection().getContentType();
            InputStream errorStream = request.connection().getErrorStream();
            if (errorStream != null) request.attachResponseStream(errorStream);
            this.ensureWithinDeadline(request, deadline);
            IOException cleanupFailure = request.terminate(Termination.COMPLETED);
            if (cleanupFailure != null) throw cleanupFailure;
            return new ResponseMetadata(responseCode, mimeType);
        } catch (IOException exception) {
            addCleanupFailure(exception, request.terminate(Termination.FAILED));
            throw request.mapIOException(exception);
        } catch (RuntimeException | Error failure) {
            addCleanupFailure(failure, request.terminate(Termination.FAILED));
            throw failure;
        }
    }

    @NotNull
    private ActiveRequest startRequest(@NotNull URI resourceUri, @NotNull String method, @NotNull RequestLimits limits, @NotNull RequestDeadline deadline) throws IOException {
        deadline.requireRemaining();
        synchronized (this.lifecycleLock) {
            if (this.closed) throw new IOException("Web resource client is shut down");
        }
        HttpURLConnection connection;
        try {
            connection = Objects.requireNonNull(this.connectionFactory.open(resourceUri), "connectionFactory result");
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Failed to create HTTP connection", exception);
        }

        ActiveRequest request = new ActiveRequest(connection);
        if (!this.register(request)) {
            request.terminate(Termination.CLIENT_SHUTDOWN);
            throw new IOException("Web resource client is shut down");
        }

        try {
            connection.setConnectTimeout(deadline.boundedTimeoutMillis(limits.connectTimeout()));
            connection.setReadTimeout(deadline.boundedTimeoutMillis(limits.readTimeout()));
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod(method);
            request.setDeadlineTask(this.deadlineScheduler.schedule(() -> request.terminate(Termination.DEADLINE_EXCEEDED), deadline.remainingDuration()));
            request.ensureOpen();
            return request;
        } catch (IOException exception) {
            addCleanupFailure(exception, request.terminate(Termination.FAILED));
            throw request.mapIOException(exception);
        } catch (RuntimeException exception) {
            IOException wrapped = new IOException("Failed to configure bounded HTTP request", exception);
            addCleanupFailure(wrapped, request.terminate(Termination.FAILED));
            throw request.mapIOException(wrapped);
        } catch (Error error) {
            addCleanupFailure(error, request.terminate(Termination.FAILED));
            throw error;
        }
    }

    private int getResponseCode(@NotNull ActiveRequest request, @NotNull RequestDeadline deadline) throws IOException {
        request.ensureOpen();
        deadline.requireRemaining();
        try {
            int responseCode = request.connection().getResponseCode();
            this.ensureWithinDeadline(request, deadline);
            return responseCode;
        } catch (IOException exception) {
            throw request.mapIOException(exception);
        }
    }

    private void ensureWithinDeadline(@NotNull ActiveRequest request, @NotNull RequestDeadline deadline) throws IOException {
        if (deadline.isExpired()) request.terminate(Termination.DEADLINE_EXCEEDED);
        request.ensureOpen();
    }

    private boolean register(@NotNull ActiveRequest request) {
        synchronized (this.lifecycleLock) {
            if (this.closed) return false;
            this.activeRequests.add(request);
            return true;
        }
    }

    private void unregister(@NotNull ActiveRequest request) {
        synchronized (this.lifecycleLock) {
            this.activeRequests.remove(request);
        }
    }

    private static void addCleanupFailure(@NotNull Throwable primary, @Nullable IOException cleanupFailure) {
        if ((cleanupFailure != null) && (cleanupFailure != primary)) primary.addSuppressed(cleanupFailure);
    }

    record RequestLimits(@NotNull Duration connectTimeout, @NotNull Duration readTimeout, @NotNull Duration overallTimeout, long maximumBytes) {

        RequestLimits {
            connectTimeout = requirePositive(connectTimeout, "connectTimeout");
            readTimeout = requirePositive(readTimeout, "readTimeout");
            overallTimeout = requirePositive(overallTimeout, "overallTimeout");
            if (maximumBytes <= 0L) throw new IllegalArgumentException("maximumBytes must be positive");
        }

        private static @NotNull Duration requirePositive(@NotNull Duration duration, @NotNull String name) {
            Duration checked = Objects.requireNonNull(duration, name);
            if (checked.isNegative() || checked.isZero()) throw new IllegalArgumentException(name + " must be positive");
            return checked;
        }
    }

    @FunctionalInterface
    interface ConnectionFactory {

        @NotNull HttpURLConnection open(@NotNull URI resourceUri) throws IOException;
    }

    private record ResponseMetadata(int responseCode, @Nullable String mimeType) {
    }

    private enum Termination {
        OPEN,
        COMPLETED,
        USER_CLOSED,
        FAILED,
        SIZE_LIMIT_EXCEEDED,
        DEADLINE_EXCEEDED,
        CLIENT_SHUTDOWN
    }

    private final class ActiveRequest {

        private final HttpURLConnection connection;
        private final AtomicReference<Termination> termination = new AtomicReference<>(Termination.OPEN);
        private final AtomicReference<InputStream> responseStream = new AtomicReference<>();
        private final AtomicReference<DeadlineScheduler.ScheduledTask> deadlineTask = new AtomicReference<>();
        private final AtomicBoolean responseStreamClosed = new AtomicBoolean();
        private final AtomicBoolean disconnected = new AtomicBoolean();

        private ActiveRequest(@NotNull HttpURLConnection connection) {
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        private @NotNull HttpURLConnection connection() {
            return this.connection;
        }

        private void setDeadlineTask(@NotNull DeadlineScheduler.ScheduledTask task) {
            DeadlineScheduler.ScheduledTask checkedTask = Objects.requireNonNull(task, "task");
            if (!this.deadlineTask.compareAndSet(null, checkedTask)) throw new IllegalStateException("A deadline task is already registered");
            if (this.termination.get() != Termination.OPEN) this.cancelDeadlineTask();
        }

        private void attachResponseStream(@NotNull InputStream stream) throws IOException {
            InputStream checkedStream = Objects.requireNonNull(stream, "stream");
            if (!this.responseStream.compareAndSet(null, checkedStream)) throw new IllegalStateException("A response stream is already registered");
            if (this.termination.get() != Termination.OPEN) {
                IOException closeFailure = this.closeResponseStream();
                IOException terminated = this.terminationException();
                addCleanupFailure(terminated, closeFailure);
                throw terminated;
            }
        }

        private void ensureOpen() throws IOException {
            if (this.termination.get() != Termination.OPEN) throw this.terminationException();
        }

        private @NotNull IOException mapIOException(@NotNull IOException exception) {
            Termination reason = this.termination.get();
            if (reason == Termination.DEADLINE_EXCEEDED) {
                if (exception instanceof SocketTimeoutException) return exception;
                SocketTimeoutException timeout = new SocketTimeoutException("Web resource request exceeded its overall deadline");
                timeout.initCause(exception);
                return timeout;
            }
            if (reason == Termination.CLIENT_SHUTDOWN) return new IOException("Web resource client shut down while the request was active", exception);
            return exception;
        }

        private @NotNull IOException terminationException() {
            Termination reason = this.termination.get();
            if (reason == Termination.DEADLINE_EXCEEDED) return new SocketTimeoutException("Web resource request exceeded its overall deadline");
            if (reason == Termination.CLIENT_SHUTDOWN) return new IOException("Web resource client shut down while the request was active");
            if (reason == Termination.SIZE_LIMIT_EXCEEDED) return new IOException("Web resource exceeded its configured size limit");
            return new IOException("Web resource request is closed");
        }

        private @Nullable IOException terminate(@NotNull Termination reason) {
            if (reason == Termination.OPEN) throw new IllegalArgumentException("Cannot terminate a request with OPEN state");
            if (!this.termination.compareAndSet(Termination.OPEN, reason)) return null;
            IOException failure = null;
            try {
                this.cancelDeadlineTask();
                failure = this.disconnect();
                IOException streamCloseFailure = this.closeResponseStream();
                if (failure == null) failure = streamCloseFailure;
                else if (streamCloseFailure != null) failure.addSuppressed(streamCloseFailure);
            } finally {
                BoundedWebResourceClient.this.unregister(this);
            }
            return failure;
        }

        private void cancelDeadlineTask() {
            DeadlineScheduler.ScheduledTask task = this.deadlineTask.getAndSet(null);
            if (task == null) return;
            try {
                task.cancel(false);
            } catch (RuntimeException ignored) {}
        }

        private @Nullable IOException closeResponseStream() {
            InputStream stream = this.responseStream.get();
            if ((stream == null) || !this.responseStreamClosed.compareAndSet(false, true)) return null;
            try {
                stream.close();
                return null;
            } catch (IOException exception) {
                return exception;
            } catch (RuntimeException exception) {
                return new IOException("Failed to close HTTP response stream", exception);
            }
        }

        private @Nullable IOException disconnect() {
            if (!this.disconnected.compareAndSet(false, true)) return null;
            try {
                this.connection.disconnect();
                return null;
            } catch (RuntimeException exception) {
                return new IOException("Failed to disconnect HTTP response", exception);
            }
        }
    }

    private static final class RequestDeadline {

        private final long deadlineNanos;
        private final LongSupplier nanoTimeSource;

        private RequestDeadline(@NotNull Duration timeout, @NotNull LongSupplier nanoTimeSource) {
            this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
            this.deadlineNanos = this.nanoTimeSource.getAsLong() + Objects.requireNonNull(timeout, "timeout").toNanos();
        }

        private boolean isExpired() {
            return this.remainingNanos() <= 0L;
        }

        private void requireRemaining() throws SocketTimeoutException {
            this.requireRemainingNanos();
        }

        private int boundedTimeoutMillis(@NotNull Duration configuredTimeout) throws SocketTimeoutException {
            long timeoutNanos = Math.min(Objects.requireNonNull(configuredTimeout, "configuredTimeout").toNanos(), this.requireRemainingNanos());
            long timeoutMillis = TimeUnit.NANOSECONDS.toMillis(timeoutNanos - 1L) + 1L;
            return (int)Math.min(Integer.MAX_VALUE, Math.max(1L, timeoutMillis));
        }

        private @NotNull Duration remainingDuration() throws SocketTimeoutException {
            return Duration.ofNanos(this.requireRemainingNanos());
        }

        private long requireRemainingNanos() throws SocketTimeoutException {
            long remainingNanos = this.remainingNanos();
            if (remainingNanos <= 0L) throw new SocketTimeoutException("Web resource request exceeded its overall deadline");
            return remainingNanos;
        }

        private long remainingNanos() {
            return this.deadlineNanos - this.nanoTimeSource.getAsLong();
        }
    }

    private static final class BoundedResponseInputStream extends InputStream {

        private final InputStream delegate;
        private final ActiveRequest request;
        private final RequestDeadline deadline;
        private final RequestLimits limits;
        private final AtomicBoolean closed = new AtomicBoolean();
        private long bytesRead;
        private boolean endOfStream;

        private BoundedResponseInputStream(@NotNull InputStream delegate, @NotNull ActiveRequest request, @NotNull RequestDeadline deadline, @NotNull RequestLimits limits) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.request = Objects.requireNonNull(request, "request");
            this.deadline = Objects.requireNonNull(deadline, "deadline");
            this.limits = Objects.requireNonNull(limits, "limits");
        }

        @Override
        public int read() throws IOException {
            if (this.endOfStream) return -1;
            try {
                this.prepareRead();
                int value = this.delegate.read();
                this.verifyAfterRead();
                if (value == -1) return this.finishEndOfStream();
                if (this.bytesRead >= this.limits.maximumBytes()) throw this.sizeLimitExceeded(this.bytesRead + 1L);
                this.bytesRead++;
                return value;
            } catch (ResourceSizeLimitException exception) {
                addCleanupFailure(exception, this.request.terminate(Termination.SIZE_LIMIT_EXCEEDED));
                throw exception;
            } catch (IOException exception) {
                addCleanupFailure(exception, this.request.terminate(Termination.FAILED));
                throw this.request.mapIOException(exception);
            } catch (RuntimeException | Error failure) {
                addCleanupFailure(failure, this.request.terminate(Termination.FAILED));
                throw failure;
            }
        }

        @Override
        public int read(byte @NotNull [] buffer, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, buffer.length);
            if (length == 0) return 0;
            if (this.endOfStream) return -1;
            try {
                this.prepareRead();
                long remainingBytes = this.limits.maximumBytes() - this.bytesRead;
                if (remainingBytes <= 0L) {
                    int probe = this.delegate.read();
                    this.verifyAfterRead();
                    if (probe == -1) return this.finishEndOfStream();
                    throw this.sizeLimitExceeded(this.bytesRead + 1L);
                }
                int boundedLength = (int)Math.min(length, remainingBytes);
                int count = this.delegate.read(buffer, offset, boundedLength);
                this.verifyAfterRead();
                if (count == -1) return this.finishEndOfStream();
                this.bytesRead += count;
                return count;
            } catch (ResourceSizeLimitException exception) {
                addCleanupFailure(exception, this.request.terminate(Termination.SIZE_LIMIT_EXCEEDED));
                throw exception;
            } catch (IOException exception) {
                addCleanupFailure(exception, this.request.terminate(Termination.FAILED));
                throw this.request.mapIOException(exception);
            } catch (RuntimeException | Error failure) {
                addCleanupFailure(failure, this.request.terminate(Termination.FAILED));
                throw failure;
            }
        }

        @Override
        public int available() throws IOException {
            if (this.endOfStream) return 0;
            try {
                this.prepareRead();
                long remainingBytes = this.limits.maximumBytes() - this.bytesRead;
                int available = (int)Math.min(Math.max(0L, remainingBytes), this.delegate.available());
                this.verifyAfterRead();
                return available;
            } catch (IOException exception) {
                addCleanupFailure(exception, this.request.terminate(Termination.FAILED));
                throw this.request.mapIOException(exception);
            } catch (RuntimeException | Error failure) {
                addCleanupFailure(failure, this.request.terminate(Termination.FAILED));
                throw failure;
            }
        }

        @Override
        public void close() throws IOException {
            if (!this.closed.compareAndSet(false, true)) return;
            IOException failure = this.request.terminate(Termination.USER_CLOSED);
            if (failure != null) throw failure;
        }

        private void prepareRead() throws IOException {
            if (this.closed.get()) throw new IOException("Web resource response stream is closed");
            if (this.deadline.isExpired()) this.request.terminate(Termination.DEADLINE_EXCEEDED);
            this.request.ensureOpen();
            try {
                this.request.connection().setReadTimeout(this.deadline.boundedTimeoutMillis(this.limits.readTimeout()));
            } catch (IllegalStateException | UnsupportedOperationException ignored) {
                // Some URLConnection implementations do not accept timeout changes after connect. The independently
                // scheduled watchdog remains authoritative and will disconnect a read at the absolute deadline.
            }
        }

        private void verifyAfterRead() throws IOException {
            if (this.deadline.isExpired()) this.request.terminate(Termination.DEADLINE_EXCEEDED);
            this.request.ensureOpen();
        }

        private int finishEndOfStream() throws IOException {
            this.endOfStream = true;
            IOException closeFailure = this.request.terminate(Termination.COMPLETED);
            if (closeFailure != null) throw closeFailure;
            return -1;
        }

        private @NotNull ResourceSizeLimitException sizeLimitExceeded(long observedBytes) {
            return new ResourceSizeLimitException(this.limits.maximumBytes(), observedBytes);
        }
    }

    private static final class ResourceSizeLimitException extends IOException {

        private ResourceSizeLimitException(long maximumBytes, long observedBytes) {
            super("Web resource exceeds the " + maximumBytes + " byte limit (observed at least " + observedBytes + " bytes)");
        }
    }
}
