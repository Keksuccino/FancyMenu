package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Thread-safe progress of a long-running profile operation, measured in processed bytes.
 */
public class ProfileTaskProgress {

    /** No time estimation happens before this much time passed, because early throughput values are too noisy. */
    private static final long MIN_ESTIMATION_TIME_MS = 750L;

    private final AtomicLong totalBytes = new AtomicLong();
    private final AtomicLong processedBytes = new AtomicLong();
    private final AtomicBoolean cancelRequested = new AtomicBoolean();
    @NotNull
    private final LongSupplier clockMs;
    private final long startTimeMs;

    public ProfileTaskProgress() {
        this(System::currentTimeMillis);
    }

    public ProfileTaskProgress(@NotNull LongSupplier clockMs) {
        this.clockMs = clockMs;
        this.startTimeMs = clockMs.getAsLong();
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes.set(Math.max(0L, totalBytes));
    }

    public void addTotalBytes(long bytes) {
        this.totalBytes.addAndGet(Math.max(0L, bytes));
    }

    public long getTotalBytes() {
        return this.totalBytes.get();
    }

    public void addProcessedBytes(long bytes) {
        this.processedBytes.addAndGet(Math.max(0L, bytes));
    }

    public long getProcessedBytes() {
        return this.processedBytes.get();
    }

    /**
     * @return the progress between 0 and 1.
     */
    public float getProgress() {
        long total = this.totalBytes.get();
        if (total <= 0L) return 0.0F;
        return (float) Math.min(1.0D, (double) this.processedBytes.get() / (double) total);
    }

    /**
     * Estimates the remaining time based on the average throughput so far.
     *
     * @return the estimated remaining milliseconds, or -1 if there is no reliable estimation yet.
     */
    public long getEstimatedRemainingMs() {
        long elapsed = this.clockMs.getAsLong() - this.startTimeMs;
        long processed = this.processedBytes.get();
        long total = this.totalBytes.get();
        if ((elapsed < MIN_ESTIMATION_TIME_MS) || (processed <= 0L) || (total <= 0L)) return -1L;
        double bytesPerMs = (double) processed / (double) elapsed;
        return Math.max(0L, (long) Math.ceil((total - Math.min(processed, total)) / bytesPerMs));
    }

    public void requestCancel() {
        this.cancelRequested.set(true);
    }

    public boolean isCancelRequested() {
        return this.cancelRequested.get();
    }

    /**
     * @throws CancellationException if cancelling got requested.
     */
    public void throwIfCancelled() {
        if (this.cancelRequested.get()) throw new CancellationException("Profile task got cancelled!");
    }

}
