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

}
