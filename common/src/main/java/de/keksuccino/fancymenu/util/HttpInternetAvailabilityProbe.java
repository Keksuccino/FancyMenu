package de.keksuccino.fancymenu.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class HttpInternetAvailabilityProbe implements InternetAvailabilityProbe {

    private final URI endpoint;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ConnectionFactory connectionFactory;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<ActiveAttempt> activeAttempt = new AtomicReference<>();

    HttpInternetAvailabilityProbe(@NotNull URI endpoint, int connectTimeoutMillis, int readTimeoutMillis, @NotNull ConnectionFactory connectionFactory) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (connectTimeoutMillis < 0) throw new IllegalArgumentException("connectTimeoutMillis must not be negative");
        if (readTimeoutMillis < 0) throw new IllegalArgumentException("readTimeoutMillis must not be negative");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public boolean isAvailable() {
        if (this.closed.get()) return false;

        ActiveAttempt attempt;
        try {
            attempt = new ActiveAttempt(this.connectionFactory.open(this.endpoint));
        } catch (IOException ex) {
            return false;
        }

        // Closing can race between the first closed check and active-attempt publication. Publishing first and
        // rechecking afterwards guarantees either close() or this thread owns the exactly-once disconnect.
        if (!this.activeAttempt.compareAndSet(null, attempt)) {
            attempt.disconnect();
            return false;
        }
        if (this.closed.get()) {
            this.activeAttempt.compareAndSet(attempt, null);
            attempt.disconnect();
            return false;
        }

        try {
            HttpURLConnection connection = attempt.connection();
            connection.setConnectTimeout(this.connectTimeoutMillis);
            connection.setReadTimeout(this.readTimeoutMillis);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return !this.closed.get() && responseCode >= 200 && responseCode < 300;
        } catch (IOException ex) {
            return false;
        } finally {
            this.activeAttempt.compareAndSet(attempt, null);
            attempt.disconnect();
        }
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) return;
        ActiveAttempt attempt = this.activeAttempt.getAndSet(null);
        if (attempt != null) attempt.disconnect();
    }

    @FunctionalInterface
    interface ConnectionFactory {

        @NotNull HttpURLConnection open(@NotNull URI endpoint) throws IOException;
    }

    private static final class ActiveAttempt {

        private final HttpURLConnection connection;
        private final AtomicBoolean disconnected = new AtomicBoolean();

        private ActiveAttempt(@NotNull HttpURLConnection connection) {
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        private @NotNull HttpURLConnection connection() {
            return this.connection;
        }

        private void disconnect() {
            if (this.disconnected.compareAndSet(false, true)) this.connection.disconnect();
        }
    }
}
