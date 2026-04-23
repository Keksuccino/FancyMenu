package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FFMPEGInstallation {

    private final @NotNull File installDirectory;
    private final @NotNull File ffmpegBinary;
    private final @NotNull File ffprobeBinary;
    private final @NotNull String platformId;
    private final @NotNull String providerId;
    private final @NotNull String ffmpegVersionLine;
    private final @NotNull String ffprobeVersionLine;

    public FFMPEGInstallation(
            @NotNull File installDirectory,
            @NotNull File ffmpegBinary,
            @NotNull File ffprobeBinary,
            @NotNull String platformId,
            @NotNull String providerId,
            @NotNull String ffmpegVersionLine,
            @NotNull String ffprobeVersionLine
    ) {
        this.installDirectory = installDirectory;
        this.ffmpegBinary = ffmpegBinary;
        this.ffprobeBinary = ffprobeBinary;
        this.platformId = platformId;
        this.providerId = providerId;
        this.ffmpegVersionLine = ffmpegVersionLine;
        this.ffprobeVersionLine = ffprobeVersionLine;
    }

    public @NotNull File getInstallDirectory() {
        return this.installDirectory;
    }

    public @NotNull File getFfmpegBinary() {
        return this.ffmpegBinary;
    }

    public @NotNull File getFfprobeBinary() {
        return this.ffprobeBinary;
    }

    public @NotNull String getPlatformId() {
        return this.platformId;
    }

    public @NotNull String getProviderId() {
        return this.providerId;
    }

    public @NotNull String getFfmpegVersionLine() {
        return this.ffmpegVersionLine;
    }

    public @NotNull String getFfprobeVersionLine() {
        return this.ffprobeVersionLine;
    }

    public boolean isValid() {
        return this.installDirectory.isDirectory() && this.ffmpegBinary.isFile() && this.ffprobeBinary.isFile();
    }

}
