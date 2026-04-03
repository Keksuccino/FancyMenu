package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AfmaEncodeJob {

    private final @NotNull Kind kind;
    private final @NotNull AtomicBoolean cancelled = new AtomicBoolean(false);
    private final @NotNull AtomicReference<AfmaEncodeProgress> progress = new AtomicReference<>(AfmaEncodeProgress.idle());
    private final long startedAtMillis = System.currentTimeMillis();
    private volatile @NotNull Status status = Status.RUNNING;
    private volatile @Nullable Throwable failure;
    private volatile @Nullable AfmaCreatorAnalysisResult analysisResult;
    private volatile @Nullable File outputFile;
    private volatile long finishedAtMillis = -1L;

    public AfmaEncodeJob(@NotNull Kind kind) {
        this.kind = Objects.requireNonNull(kind);
    }

    public @NotNull Kind getKind() {
        return this.kind;
    }

    public void cancel() {
        this.cancelled.set(true);
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.CANCELLED, "Cancelling AFMA job...", null, this.progress.get().progress()));
    }

    public boolean isCancellationRequested() {
        return this.cancelled.get();
    }

    public @NotNull AfmaEncodeProgress getProgress() {
        return this.progress.get();
    }

    public void setProgress(@NotNull AfmaEncodeProgress progress) {
        this.progress.set(Objects.requireNonNull(progress));
    }

    public @NotNull Status getStatus() {
        return this.status;
    }

    public boolean isRunning() {
        return this.status == Status.RUNNING;
    }

    public @Nullable Throwable getFailure() {
        return this.failure;
    }

    public @Nullable AfmaCreatorAnalysisResult getAnalysisResult() {
        return this.analysisResult;
    }

    public @Nullable File getOutputFile() {
        return this.outputFile;
    }

    public long getElapsedMillis() {
        long endMillis = (this.finishedAtMillis >= 0L) ? this.finishedAtMillis : System.currentTimeMillis();
        return Math.max(0L, endMillis - this.startedAtMillis);
    }

    public @Nullable Long getEstimatedRemainingMillis() {
        if (!this.isRunning()) {
            return 0L;
        }
        double progressValue = this.progress.get().progress();
        if (progressValue < 0.02D || progressValue >= 0.995D) {
            return null;
        }
        long elapsedMillis = this.getElapsedMillis();
        long estimatedTotalMillis = Math.max(elapsedMillis, Math.round(elapsedMillis / progressValue));
        return Math.max(0L, estimatedTotalMillis - elapsedMillis);
    }

    public void completeSuccess(@Nullable AfmaCreatorAnalysisResult analysisResult, @Nullable File outputFile) {
        this.analysisResult = analysisResult;
        this.outputFile = outputFile;
        this.status = Status.SUCCEEDED;
        this.finishedAtMillis = System.currentTimeMillis();
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.COMPLETE, "AFMA job finished.", null, 1.0D));
    }

    public void completeCancelled() {
        this.status = Status.CANCELLED;
        this.finishedAtMillis = System.currentTimeMillis();
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.CANCELLED, "AFMA job cancelled.", null, this.progress.get().progress()));
    }

    public void completeFailure(@NotNull Throwable throwable) {
        this.failure = Objects.requireNonNull(throwable);
        this.status = Status.FAILED;
        this.finishedAtMillis = System.currentTimeMillis();
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.FAILED, "AFMA job failed.", throwable.getMessage(), this.progress.get().progress()));
    }

    public enum Kind {
        ANALYZE,
        EXPORT
    }

    public enum Status {
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

}
