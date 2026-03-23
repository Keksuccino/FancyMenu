package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGDownloader;
import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGInstallation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            return "Optional / not installed";
        }
        return installation.getProviderId() + " | " + installation.getPlatformId();
    }

    public @NotNull String describeBinaryPath() {
        FFMPEGInstallation installation = this.resolveInstallation();
        if (installation == null) {
            return "Unavailable (not needed for PNG sequence export)";
        }
        return installation.getFfmpegBinary().getAbsolutePath();
    }

}
