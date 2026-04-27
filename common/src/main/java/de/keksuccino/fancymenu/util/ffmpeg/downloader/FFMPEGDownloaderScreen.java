package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class FFMPEGDownloaderScreen extends Screen {

    private final @NotNull Consumer<FFMPEGDownloaderScreenResult> onClosed;
    private final boolean autoStart;

    private @Nullable ExtendedButton actionButton;
    private @Nullable ExtendedButton cancelButton;
    private @Nullable ExtendedButton openFolderButton;
    private @Nullable ExtendedButton closeButton;
    private boolean startRequested;

    public FFMPEGDownloaderScreen(@NotNull Consumer<FFMPEGDownloaderScreenResult> onClosed) {
        this(onClosed, true);
    }

    public FFMPEGDownloaderScreen(@NotNull Consumer<FFMPEGDownloaderScreenResult> onClosed, boolean autoStart) {
        super(Component.translatable("fancymenu.ffmpeg.downloader.title").withStyle(ChatFormatting.GOLD));
        this.onClosed = Objects.requireNonNull(onClosed);
        this.autoStart = autoStart;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height - 34;

        this.actionButton = this.addRenderableWidget(new ExtendedButton(centerX - 154, buttonY, 100, 20, Component.translatable("fancymenu.ffmpeg.downloader.start"), button -> this.startDownload()));
        this.cancelButton = this.addRenderableWidget(new ExtendedButton(centerX - 50, buttonY, 100, 20, Component.translatable("fancymenu.ffmpeg.downloader.cancel"), button -> FFMPEGDownloader.cancelCurrentDownload()));
        this.openFolderButton = this.addRenderableWidget(new ExtendedButton(centerX + 54, buttonY, 120, 20, Component.translatable("fancymenu.ffmpeg.downloader.open_folder"), button -> {
            File installationDir = FFMPEGDownloader.getInstalledDirectory();
            if (installationDir == null) {
                installationDir = FFMPEGDownloader.getDownloadDirectory();
            }
            FileUtils.openFile(installationDir);
        }));
        this.closeButton = this.addRenderableWidget(new ExtendedButton(centerX + 178, buttonY, 100, 20, Component.translatable("fancymenu.common.close"), button -> this.onClose()));

        this.updateButtonStates(FFMPEGDownloader.getSnapshot());

        if (this.autoStart && !this.startRequested) {
            this.startDownload();
        }
    }

    private void startDownload() {
        this.startRequested = true;
        FFMPEGDownloader.startDownloadIfNeededAsync();
    }

    @Override
    public void tick() {
        super.tick();

        FFMPEGDownloadSnapshot snapshot = FFMPEGDownloader.getSnapshot();
        this.updateButtonStates(snapshot);
    }

    private void updateButtonStates(@NotNull FFMPEGDownloadSnapshot snapshot) {
        if ((this.actionButton == null) || (this.cancelButton == null) || (this.openFolderButton == null) || (this.closeButton == null)) {
            return;
        }

        boolean active = snapshot.isActive();
        boolean success = snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.COMPLETE;
        boolean failed = snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.FAILED;
        boolean cancelled = snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.CANCELLED;
        boolean cached = snapshot.isSatisfiedByCache();

        this.actionButton.visible = !active && !success;
        this.actionButton.active = !active;
        this.actionButton.setMessage(Component.translatable((failed || cancelled) ? "fancymenu.ffmpeg.downloader.retry" : "fancymenu.ffmpeg.downloader.start"));

        this.cancelButton.visible = active;
        this.cancelButton.active = active;

        this.closeButton.visible = !active;
        this.closeButton.active = !active;

        this.openFolderButton.visible = !active;
        this.openFolderButton.active = cached || success || (FFMPEGDownloader.getCachedInstallation() != null) || FFMPEGDownloader.getDownloadDirectory().isDirectory();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        FFMPEGDownloadSnapshot snapshot = FFMPEGDownloader.getSnapshot();
        double cx = this.width / 2.0D;
        double cy = this.height / 2.0D;
        int progressBarWidth = this.width / 3;
        int progressBarHeight = 14;

        Matrix3x2fStack pose = graphics.pose();

        pose.pushMatrix();
        pose.translate((float) cx, (float) cy);
        pose.translate((float) (-progressBarWidth / 2.0D), (float) (-progressBarHeight / 2.0D));
        graphics.fill(0, 0, progressBarWidth, progressBarHeight, -1);
        graphics.fill(2, 2, progressBarWidth - 2, progressBarHeight - 2, -16777215);
        int fillWidth = (int) ((progressBarWidth - 4) * snapshot.getProgress());
        if (fillWidth > 0) {
            graphics.fill(4, 4, 4 + fillWidth, progressBarHeight - 4, -1);
        }
        pose.popMatrix();

        List<Component> textLines = buildTextLines(snapshot);
        int offset = ((this.font.lineHeight / 2) + ((this.font.lineHeight + 2) * (textLines.size() + 2))) + 4;
        pose.pushMatrix();
        pose.translate((float) cx, (float) (cy - offset));
        graphics.text(this.font, this.title, (int) -(this.font.width(this.title) / 2.0D), 0, -1);
        for (Component line : textLines) {
            pose.translate(0.0F, this.font.lineHeight + 2.0F);
            graphics.text(this.font, line, (int) -(this.font.width(line) / 2.0D), 0, -1);
        }
        pose.popMatrix();

        if (snapshot.getFailureMessage() != null && !snapshot.getFailureMessage().isBlank()) {
            int infoMaxWidth = Math.min(this.width - 40, 420);
            int infoX = (this.width / 2) - (infoMaxWidth / 2);
            int infoY = (int) cy + 36;
            List<MutableComponent> wrappedFailure = TextFormattingUtils.lineWrapComponents(Component.literal(snapshot.getFailureMessage()).withStyle(ChatFormatting.RED), infoMaxWidth);
            for (MutableComponent line : wrappedFailure) {
                graphics.text(this.font, line, infoX, infoY, 0xFF7A7A, false);
                infoY += this.font.lineHeight + 2;
            }
        }

    }

    private @NotNull List<Component> buildTextLines(@NotNull FFMPEGDownloadSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(snapshot.getTask()));

        String detail = snapshot.getDetail();
        if (detail != null && !detail.isBlank()) {
            lines.add(Component.literal(detail));
        }

        if (snapshot.isActive()) {
            lines.add(Component.literal(Math.round(snapshot.getProgress() * 100.0D) + "%"));
        } else if (snapshot.getStage() == FFMPEGDownloadSnapshot.Stage.COMPLETE) {
            FFMPEGInstallation installation = snapshot.getInstallation();
            if (installation != null) {
                lines.add(Component.literal(installation.getProviderId() + " | " + installation.getPlatformId()));
            }
        }

        return lines;
    }

    @Override
    public void onClose() {
        this.onClosed.accept(FFMPEGDownloaderScreenResult.fromSnapshot(FFMPEGDownloader.getSnapshot()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

}
