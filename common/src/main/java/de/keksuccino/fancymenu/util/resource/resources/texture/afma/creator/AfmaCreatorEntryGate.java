package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGDownloaderScreen;
import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGDownloaderScreenResult;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AfmaCreatorEntryGate {

    private AfmaCreatorEntryGate() {
    }

    public static void open(@NotNull Screen parentScreen) {
        Objects.requireNonNull(parentScreen);

        AfmaFfmpegBridge bridge = new AfmaFfmpegBridge();
        if (bridge.isReady()) {
            Minecraft.getInstance().setScreen(new AfmaCreatorScreen(parentScreen));
            return;
        }

        Minecraft.getInstance().setScreen(new FFMPEGDownloaderScreen(result -> handleDownloaderResult(parentScreen, result), true));
    }

    private static void handleDownloaderResult(@NotNull Screen parentScreen, @NotNull FFMPEGDownloaderScreenResult result) {
        if (result.isReady()) {
            Minecraft.getInstance().setScreen(new AfmaCreatorScreen(parentScreen));
            return;
        }

        Minecraft.getInstance().setScreen(parentScreen);

        if (result.outcome() == FFMPEGDownloaderScreenResult.Outcome.FAILED) {
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.afma.creator.ffmpeg_required.failed_retry"), MessageDialogStyle.WARNING, retry -> {
                if (retry) {
                    open(parentScreen);
                }
            });
        } else if (result.outcome() == FFMPEGDownloaderScreenResult.Outcome.CANCELLED) {
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.afma.creator.ffmpeg_required.cancelled_retry"), MessageDialogStyle.WARNING, retry -> {
                if (retry) {
                    open(parentScreen);
                }
            });
        }
    }

}
