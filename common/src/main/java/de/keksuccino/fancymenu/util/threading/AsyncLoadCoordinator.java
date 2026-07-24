package de.keksuccino.fancymenu.util.threading;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Coordinates one asynchronous load per key and gives every admitted load a stable identity.
 * Cache mutation callbacks run under the same short lifecycle lock as admission, timeout, and
 * reset so a cancelled or superseded load can never publish into newer state.
 */
public final class AsyncLoadCoordinator<K> {

    private final Object lifecycleLock = new Object();
    private final Map<K, Claim<K>> inFlight = new HashMap<>();
    private long generation;

    /**
     * Atomically rechecks the caller's cache state and claims the key when work is still needed.
     * The predicate must stay fast and must not call back into this coordinator.
     */
    public @Nullable Claim<K> tryClaim(@NotNull K key, long startedAt, @NotNull BooleanSupplier admissionCheck) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(admissionCheck, "admissionCheck");
        synchronized (this.lifecycleLock) {
            if (this.inFlight.containsKey(key) || !admissionCheck.getAsBoolean()) return null;
            Claim<K> claim = new Claim<>(this, key, this.generation, startedAt);
            this.inFlight.put(key, claim);
            return claim;
        }
    }

    /**
     * Runs an admitted task at most once. Cancellation before execution skips the task, while
     * cancellation during execution interrupts its runner and still relies on identity-checked
     * publication for APIs that ignore interruption.
     */
    public void runClaim(@NotNull Claim<K> claim, @NotNull Runnable task) {
        this.requireOwnedClaim(claim);
        Objects.requireNonNull(task, "task");
        if (!claim.begin()) return;
        try {
            if (!claim.isCancelled()) task.run();
        } finally {
            claim.finish();
            this.release(claim);
        }
    }

    /**
     * Publishes and releases the exact claim as one operation. Timeout/reset therefore either
     * wins before publication or observes an already completed load, never an intermediate state.
     */
    public boolean publishIfCurrent(@NotNull Claim<K> claim, @NotNull Runnable publisher) {
        this.requireOwnedClaim(claim);
        Objects.requireNonNull(publisher, "publisher");
        synchronized (this.lifecycleLock) {
            if (claim.isCancelled() || (claim.generation() != this.generation) || (this.inFlight.get(claim.key()) != claim)) return false;
            try {
                publisher.run();
                return true;
            } finally {
                this.inFlight.remove(claim.key(), claim);
            }
        }
    }

    /**
     * Releases a claim whose task could not be launched, allowing the next caller to retry.
     */
    public void abandon(@NotNull Claim<K> claim) {
        this.requireOwnedClaim(claim);
        synchronized (this.lifecycleLock) {
            claim.cancel(false);
            this.inFlight.remove(claim.key(), claim);
        }
    }

    public boolean isLoading(@NotNull K key) {
        Objects.requireNonNull(key, "key");
        synchronized (this.lifecycleLock) {
            return this.inFlight.containsKey(key);
        }
    }

    /**
     * Cancels only claims whose identities are still current. An old worker's eventual release
     * cannot remove a retry admitted for the same key.
     */
    @NotNull
    public List<K> cancelTimedOut(long currentTime, long timeoutMillis) {
        if (timeoutMillis < 0L) throw new IllegalArgumentException("timeoutMillis must not be negative");
        List<K> timedOutKeys = new ArrayList<>();
        synchronized (this.lifecycleLock) {
            Iterator<Map.Entry<K, Claim<K>>> iterator = this.inFlight.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, Claim<K>> entry = iterator.next();
                Claim<K> claim = entry.getValue();
                long elapsed = currentTime - claim.startedAt();
                if ((elapsed <= timeoutMillis) || (currentTime < claim.startedAt())) continue;
                iterator.remove();
                claim.cancel(true);
                timedOutKeys.add(entry.getKey());
            }
        }
        return List.copyOf(timedOutKeys);
    }

    /**
     * Advances the generation, cancels all admitted work, and resets dependent cache state under
     * the publication lock. New claims cannot start until the reset callback has completed.
     */
    public void reset(@NotNull Runnable resetAction) {
        Objects.requireNonNull(resetAction, "resetAction");
        synchronized (this.lifecycleLock) {
            this.generation = Math.incrementExact(this.generation);
            for (Claim<K> claim : this.inFlight.values()) claim.cancel(true);
            this.inFlight.clear();
            resetAction.run();
        }
    }

    public long generationNumber() {
        synchronized (this.lifecycleLock) {
            return this.generation;
        }
    }

    private void release(@NotNull Claim<K> claim) {
        synchronized (this.lifecycleLock) {
            this.inFlight.remove(claim.key(), claim);
        }
    }

    private void requireOwnedClaim(@NotNull Claim<K> claim) {
        Objects.requireNonNull(claim, "claim");
        if (claim.owner() != this) throw new IllegalArgumentException("The claim belongs to a different coordinator");
    }

    public static final class Claim<K> {

        private final AsyncLoadCoordinator<K> owner;
        private final K key;
        private final long generation;
        private final long startedAt;
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Thread> runner = new AtomicReference<>();

        private Claim(@NotNull AsyncLoadCoordinator<K> owner, @NotNull K key, long generation, long startedAt) {
            this.owner = owner;
            this.key = key;
            this.generation = generation;
            this.startedAt = startedAt;
        }

        private boolean begin() {
            if (!this.started.compareAndSet(false, true) || this.cancelled.get()) return false;
            Thread currentThread = Thread.currentThread();
            if (!this.runner.compareAndSet(null, currentThread)) return false;
            if (!this.cancelled.get()) return true;
            this.runner.compareAndSet(currentThread, null);
            return false;
        }

        private void finish() {
            this.runner.compareAndSet(Thread.currentThread(), null);
        }

        private void cancel(boolean mayInterruptIfRunning) {
            this.cancelled.set(true);
            Thread runningThread = this.runner.get();
            if (mayInterruptIfRunning && (runningThread != null)) runningThread.interrupt();
        }

        private AsyncLoadCoordinator<K> owner() {
            return this.owner;
        }

        @NotNull
        public K key() {
            return this.key;
        }

        public long startedAt() {
            return this.startedAt;
        }

        public boolean isCancelled() {
            return this.cancelled.get();
        }

        private long generation() {
            return this.generation;
        }

    }

}
