package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGDownloader;
import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGInstallation;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class AfmaFfmpegBridge {

    @Nullable
    public FFMPEGInstallation resolveInstallation() {
        FFMPEGInstallation installation = FFMPEGDownloader.getCachedInstallation();
        if ((installation != null) && installation.isValid()) {
            return installation;
        }
        return null;
    }

    public boolean isReady() {
        return this.resolveInstallation() != null;
    }

    public @NotNull String describeStatus() {
        FFMPEGInstallation installation = this.resolveInstallation();
        if (installation == null) {
            return "Not ready";
        }
        return "Ready via " + installation.getProviderId() + " | " + installation.getPlatformId();
    }

    public @NotNull String describeBinaryPath() {
        FFMPEGInstallation installation = this.resolveInstallation();
        if (installation == null) {
            return "Unavailable until the downloader finishes successfully";
        }
        return installation.getFfmpegBinary().getAbsolutePath();
    }

    public @Nullable byte[] optimizePngPayload(@NotNull byte[] sourceBytes) throws IOException {
        Objects.requireNonNull(sourceBytes);
        FFMPEGInstallation installation = this.resolveInstallation();
        if (installation == null) {
            return null;
        }

        File tempDirectory = Files.createTempDirectory("fancymenu_afma_ffmpeg_png_").toFile();
        try {
            File inputFile = new File(tempDirectory, "input.png");
            File outputFile = new File(tempDirectory, "output.png");
            Files.write(inputFile.toPath(), sourceBytes);

            Process process = new ProcessBuilder(List.of(
                    installation.getFfmpegBinary().getAbsolutePath(),
                    "-v", "error",
                    "-y",
                    "-i", inputFile.getAbsolutePath(),
                    "-frames:v", "1",
                    outputFile.getAbsolutePath()
            ))
                    .redirectErrorStream(true)
                    .start();

            String processOutput;
            try (InputStream processInput = process.getInputStream()) {
                processOutput = new String(processInput.readAllBytes(), StandardCharsets.UTF_8);
            }

            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("AFMA FFmpeg payload optimization was interrupted", ex);
            }

            if (exitCode != 0) {
                throw new IOException("FFmpeg failed to optimize an AFMA payload PNG: " + processOutput.trim());
            }
            if (!outputFile.isFile()) {
                throw new IOException("FFmpeg did not produce an optimized AFMA payload PNG");
            }

            byte[] optimizedBytes = Files.readAllBytes(outputFile.toPath());
            return (optimizedBytes.length < sourceBytes.length) ? optimizedBytes : null;
        } finally {
            FileUtils.deleteQuietly(tempDirectory);
        }
    }

}
