package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owns queued and in-flight outbound payload accounting. Byte totals include the fixed protocol envelope that is
 * added at send time. Moving an entry into the single in-flight slot does not release its budget; only successful
 * delivery, expiry, rejection, or terminal cleanup does that.
 */
final class PendingPayloadBuffer {

    private final ArrayDeque<Payload> queuedPayloads = new ArrayDeque<>();
    private final int maxPendingCount;
    private final long maxPendingUtf8Bytes;
    private final long maxMessageUtf8Bytes;
    private final long messageEnvelopeUtf8Bytes;
    private final long maxAgeMillis;

    private @Nullable Payload inFlightPayload;
    private int pendingCount;
    private long pendingUtf8Bytes;

    PendingPayloadBuffer(int maxPendingCount, long maxPendingUtf8Bytes, long maxMessageUtf8Bytes, long messageEnvelopeUtf8Bytes, long maxAgeMillis) {
        if (maxPendingCount <= 0 || maxPendingUtf8Bytes <= 0L || maxMessageUtf8Bytes <= 0L || messageEnvelopeUtf8Bytes < 0L || maxAgeMillis <= 0L) {
            throw new IllegalArgumentException("Pending payload limits must be positive");
        }
        if (messageEnvelopeUtf8Bytes >= maxMessageUtf8Bytes || maxMessageUtf8Bytes > maxPendingUtf8Bytes) {
            throw new IllegalArgumentException("An enveloped message must be able to fit into the pending byte budget");
        }
        this.maxPendingCount = maxPendingCount;
        this.maxPendingUtf8Bytes = maxPendingUtf8Bytes;
        this.maxMessageUtf8Bytes = maxMessageUtf8Bytes;
        this.messageEnvelopeUtf8Bytes = messageEnvelopeUtf8Bytes;
        this.maxAgeMillis = maxAgeMillis;
    }

    @NotNull AdmissionResult offer(@NotNull String payload, long nowMillis) {
        return offer(validatePayload(payload, this.maxMessageUtf8Bytes, this.messageEnvelopeUtf8Bytes), nowMillis);
    }

    synchronized @NotNull AdmissionResult offer(@NotNull PayloadValidation validation, long nowMillis) {
        pruneExpiredQueued(nowMillis);
        if (validation.result() != AdmissionResult.ACCEPTED) {
            return validation.result();
        }
        long messageUtf8Bytes = validation.messageUtf8Bytes();
        if (this.pendingCount >= this.maxPendingCount || messageUtf8Bytes > this.maxPendingUtf8Bytes - this.pendingUtf8Bytes) {
            return AdmissionResult.CAPACITY_EXCEEDED;
        }

        this.queuedPayloads.addLast(new Payload(validation.payload(), messageUtf8Bytes, nowMillis));
        this.pendingCount++;
        this.pendingUtf8Bytes += messageUtf8Bytes;
        return AdmissionResult.ACCEPTED;
    }

    static @NotNull PayloadValidation validatePayload(@NotNull String payload, long maxMessageUtf8Bytes, long messageEnvelopeUtf8Bytes) {
        long rawPayloadUtf8Bytes = Utf8Length.count(payload);
        if (rawPayloadUtf8Bytes == Utf8Length.MALFORMED_UTF16) {
            return new PayloadValidation(payload, AdmissionResult.MALFORMED_UTF16, 0L);
        }
        if (rawPayloadUtf8Bytes > maxMessageUtf8Bytes - messageEnvelopeUtf8Bytes) {
            return new PayloadValidation(payload, AdmissionResult.PAYLOAD_TOO_LARGE, 0L);
        }
        return new PayloadValidation(payload, AdmissionResult.ACCEPTED, rawPayloadUtf8Bytes + messageEnvelopeUtf8Bytes);
    }

    synchronized @Nullable Payload pollForSend(long nowMillis) {
        pruneExpiredQueued(nowMillis);
        if (this.inFlightPayload != null) {
            return null;
        }
        this.inFlightPayload = this.queuedPayloads.pollFirst();
        return this.inFlightPayload;
    }

    synchronized boolean completeSend(@NotNull Payload payload) {
        if (this.inFlightPayload != payload) {
            return false;
        }
        this.inFlightPayload = null;
        release(payload);
        return true;
    }

    synchronized boolean retryOrDiscardSend(@NotNull Payload payload, long nowMillis, boolean retry) {
        if (this.inFlightPayload != payload) {
            return false;
        }
        this.inFlightPayload = null;
        if (retry && !isExpired(payload, nowMillis)) {
            this.queuedPayloads.addFirst(payload);
        } else {
            release(payload);
        }
        return true;
    }

    synchronized boolean discardInFlight() {
        if (this.inFlightPayload == null) {
            return false;
        }
        Payload discarded = this.inFlightPayload;
        this.inFlightPayload = null;
        release(discarded);
        return true;
    }

    synchronized int pruneExpiredQueued(long nowMillis) {
        int removed = 0;
        Iterator<Payload> iterator = this.queuedPayloads.iterator();
        while (iterator.hasNext()) {
            Payload payload = iterator.next();
            if (isExpired(payload, nowMillis)) {
                iterator.remove();
                release(payload);
                removed++;
            }
        }
        return removed;
    }

    synchronized void clear() {
        this.queuedPayloads.clear();
        this.inFlightPayload = null;
        this.pendingCount = 0;
        this.pendingUtf8Bytes = 0L;
    }

    synchronized int pendingCount() {
        return this.pendingCount;
    }

    synchronized int queuedCount() {
        return this.pendingCount - (this.inFlightPayload == null ? 0 : 1);
    }

    synchronized long pendingUtf8Bytes() {
        return this.pendingUtf8Bytes;
    }

    synchronized boolean hasInFlightPayload() {
        return this.inFlightPayload != null;
    }

    synchronized @NotNull List<String> queuedPayloadsSnapshot() {
        List<String> payloads = new ArrayList<>(this.queuedPayloads.size());
        for (Payload payload : this.queuedPayloads) {
            payloads.add(payload.payload());
        }
        return List.copyOf(payloads);
    }

    private boolean isExpired(@NotNull Payload payload, long nowMillis) {
        return nowMillis >= payload.queuedAtMillis() && nowMillis - payload.queuedAtMillis() >= this.maxAgeMillis;
    }

    private void release(@NotNull Payload payload) {
        this.pendingCount--;
        this.pendingUtf8Bytes -= payload.utf8Bytes();
        if (this.pendingCount < 0 || this.pendingUtf8Bytes < 0L) {
            throw new IllegalStateException("Pending payload accounting became negative");
        }
    }

    enum AdmissionResult {
        ACCEPTED,
        MALFORMED_UTF16,
        PAYLOAD_TOO_LARGE,
        CAPACITY_EXCEEDED
    }

    record PayloadValidation(@NotNull String payload, @NotNull AdmissionResult result, long messageUtf8Bytes) {
    }

    record Payload(@NotNull String payload, long utf8Bytes, long queuedAtMillis) {
    }
}
