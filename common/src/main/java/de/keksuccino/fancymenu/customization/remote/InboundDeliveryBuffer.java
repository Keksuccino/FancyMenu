package de.keksuccino.fancymenu.customization.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Bounds completed inbound messages while they wait for main-thread listener delivery. Overflow drops the newest
 * message, preserving FIFO order for messages that were already admitted.
 */
final class InboundDeliveryBuffer<T> {

    private final ArrayDeque<Entry<T>> entries = new ArrayDeque<>();
    private final int maxCount;
    private final long maxUtf8Bytes;

    private long queuedUtf8Bytes;

    InboundDeliveryBuffer(int maxCount, long maxUtf8Bytes) {
        if (maxCount <= 0 || maxUtf8Bytes <= 0L) {
            throw new IllegalArgumentException("Inbound delivery limits must be positive");
        }
        this.maxCount = maxCount;
        this.maxUtf8Bytes = maxUtf8Bytes;
    }

    synchronized boolean offer(@NotNull T value, long utf8Bytes) {
        if (utf8Bytes < 0L) {
            throw new IllegalArgumentException("utf8Bytes must not be negative");
        }
        if (this.entries.size() >= this.maxCount || utf8Bytes > this.maxUtf8Bytes - this.queuedUtf8Bytes) {
            return false;
        }
        this.entries.addLast(new Entry<>(value, utf8Bytes));
        this.queuedUtf8Bytes += utf8Bytes;
        return true;
    }

    synchronized @Nullable T poll() {
        Entry<T> entry = this.entries.pollFirst();
        if (entry == null) {
            return null;
        }
        this.queuedUtf8Bytes -= entry.utf8Bytes();
        return entry.value();
    }

    synchronized void clear() {
        this.entries.clear();
        this.queuedUtf8Bytes = 0L;
    }

    synchronized boolean isEmpty() {
        return this.entries.isEmpty();
    }

    synchronized int size() {
        return this.entries.size();
    }

    synchronized long queuedUtf8Bytes() {
        return this.queuedUtf8Bytes;
    }

    private record Entry<T>(@NotNull T value, long utf8Bytes) {
    }
}
