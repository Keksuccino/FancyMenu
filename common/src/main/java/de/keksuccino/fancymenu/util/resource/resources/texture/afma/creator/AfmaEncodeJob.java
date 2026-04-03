package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AfmaEncodeJob {

    private final @NotNull AtomicBoolean cancelled = new AtomicBoolean(false);
    private final @NotNull AtomicReference<AfmaEncodeProgress> progress = new AtomicReference<>(AfmaEncodeProgress.idle());
    private volatile @NotNull Status status = Status.RUNNING;
    private volatile @Nullable Throwable failure;
    private volatile @Nullable File outputFile;

    public AfmaEncodeJob() {
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

    public @Nullable File getOutputFile() {
        return this.outputFile;
    }

    public void completeSuccess(@Nullable File outputFile) {
        this.outputFile = outputFile;
        this.status = Status.SUCCEEDED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.COMPLETE, "AFMA job finished.", null, 1.0D));
    }

    public void completeCancelled() {
        this.status = Status.CANCELLED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.CANCELLED, "AFMA job cancelled.", null, this.progress.get().progress()));
    }

    public void completeFailure(@NotNull Throwable throwable) {
        this.failure = Objects.requireNonNull(throwable);
        this.status = Status.FAILED;
        this.progress.set(new AfmaEncodeProgress(AfmaEncodeProgress.Phase.FAILED, "AFMA job failed.", throwable.getMessage(), this.progress.get().progress()));
    }

    public enum Status {
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

}
