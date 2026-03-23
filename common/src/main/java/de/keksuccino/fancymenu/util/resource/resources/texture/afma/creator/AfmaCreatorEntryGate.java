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
        if (new AfmaFfmpegBridge().isReady()) {
            Minecraft.getInstance().setScreen(new AfmaCreatorScreen(parentScreen));
            return;
        }

        Minecraft.getInstance().setScreen(new FFMPEGDownloaderScreen(result -> handleDownloaderResult(parentScreen, result), true));
    }

    private static void handleDownloaderResult(@NotNull Screen parentScreen, @NotNull FFMPEGDownloaderScreenResult result) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (result.outcome()) {
            case INSTALLED, ALREADY_AVAILABLE -> minecraft.setScreen(new AfmaCreatorScreen(parentScreen));
            case FAILED -> {
                minecraft.setScreen(parentScreen);
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.afma.creator.ffmpeg_required.failed_retry"), MessageDialogStyle.ERROR, accepted -> {
                    if (accepted) {
                        open(parentScreen);
                    }
                });
            }
            case CANCELLED -> {
                minecraft.setScreen(parentScreen);
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.afma.creator.ffmpeg_required.cancelled_retry"), MessageDialogStyle.WARNING, accepted -> {
                    if (accepted) {
                        open(parentScreen);
                    }
                });
            }
            case NOT_STARTED, IN_PROGRESS -> minecraft.setScreen(parentScreen);
        }
    }

}
