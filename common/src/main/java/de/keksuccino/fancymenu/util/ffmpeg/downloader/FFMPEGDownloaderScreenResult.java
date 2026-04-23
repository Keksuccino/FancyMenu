package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FFMPEGDownloaderScreenResult(
        @NotNull Outcome outcome,
        @NotNull FFMPEGDownloadSnapshot snapshot,
        @Nullable FFMPEGInstallation installation
) {

    public static @NotNull FFMPEGDownloaderScreenResult fromSnapshot(@NotNull FFMPEGDownloadSnapshot snapshot) {
        Outcome outcome = switch (snapshot.getStage()) {
            case COMPLETE -> snapshot.isSatisfiedByCache() ? Outcome.ALREADY_AVAILABLE : Outcome.INSTALLED;
            case FAILED -> Outcome.FAILED;
            case CANCELLED -> Outcome.CANCELLED;
            case CHECKING, DOWNLOADING, EXTRACTING, VALIDATING -> Outcome.IN_PROGRESS;
            case IDLE -> Outcome.NOT_STARTED;
        };
        return new FFMPEGDownloaderScreenResult(outcome, snapshot, snapshot.getInstallation());
    }

    public boolean isReady() {
        return switch (this.outcome) {
            case INSTALLED, ALREADY_AVAILABLE -> true;
            default -> false;
        };
    }

    public enum Outcome {
        NOT_STARTED,
        IN_PROGRESS,
        INSTALLED,
        ALREADY_AVAILABLE,
        FAILED,
        CANCELLED
    }

}
