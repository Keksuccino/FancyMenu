package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FFMPEGDownloadSnapshot {

    private final @NotNull Stage stage;
    private final @NotNull String task;
    private final @Nullable String detail;
    private final double progress;
    private final long downloadedBytes;
    private final long totalBytes;
    private final @Nullable String failureMessage;
    private final @Nullable FFMPEGInstallation installation;
    private final boolean satisfiedByCache;

    public FFMPEGDownloadSnapshot(
            @NotNull Stage stage,
            @NotNull String task,
            @Nullable String detail,
            double progress,
            long downloadedBytes,
            long totalBytes,
            @Nullable String failureMessage,
            @Nullable FFMPEGInstallation installation,
            boolean satisfiedByCache
    ) {
        this.stage = stage;
        this.task = task;
        this.detail = detail;
        this.progress = Math.max(0.0D, Math.min(1.0D, progress));
        this.downloadedBytes = Math.max(0L, downloadedBytes);
        this.totalBytes = Math.max(0L, totalBytes);
        this.failureMessage = failureMessage;
        this.installation = installation;
        this.satisfiedByCache = satisfiedByCache;
    }

    public static @NotNull FFMPEGDownloadSnapshot idle() {
        return new FFMPEGDownloadSnapshot(Stage.IDLE, "Idle", null, 0.0D, 0L, 0L, null, null, false);
    }

    public @NotNull Stage getStage() {
        return this.stage;
    }

    public @NotNull String getTask() {
        return this.task;
    }

    public @Nullable String getDetail() {
        return this.detail;
    }

    public double getProgress() {
        return this.progress;
    }

    public long getDownloadedBytes() {
        return this.downloadedBytes;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public @Nullable String getFailureMessage() {
        return this.failureMessage;
    }

    public @Nullable FFMPEGInstallation getInstallation() {
        return this.installation;
    }

    public boolean isSatisfiedByCache() {
        return this.satisfiedByCache;
    }

    public boolean isActive() {
        return switch (this.stage) {
            case CHECKING, DOWNLOADING, EXTRACTING, VALIDATING -> true;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return switch (this.stage) {
            case COMPLETE, FAILED, CANCELLED -> true;
            default -> false;
        };
    }

    public enum Stage {
        IDLE,
        CHECKING,
        DOWNLOADING,
        EXTRACTING,
        VALIDATING,
        COMPLETE,
        FAILED,
        CANCELLED
    }

}
