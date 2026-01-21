package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExtractZipFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public ExtractZipFileAction() {
        super("extract_zip_file_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if ((value == null) || !value.contains("||")) {
            LOGGER.error("[FANCYMENU] ExtractZipFileAction: Invalid value: {}", value);
            return;
        }

        String[] valueArray = value.split("\\|\\|", 2);
        String rawSourceZipPath = valueArray[0];
        String rawTargetDirectoryPath = valueArray[1];

        CompletableFuture.runAsync(() -> {
            String resolvedSourceZipPath = null;
            String resolvedTargetDirectoryPath = null;
            boolean success = false;
            String failureReason = null;

            try {
                Path sourceZipPath = resolveAllowedPath(rawSourceZipPath);
                resolvedSourceZipPath = sourceZipPath.toString();

                if (!Files.exists(sourceZipPath) || Files.isDirectory(sourceZipPath)) {
                    throw new IOException("Source ZIP file not found or is a directory: " + rawSourceZipPath);
                }

                Path targetDirectoryPath = resolveAllowedPath(rawTargetDirectoryPath);
                resolvedTargetDirectoryPath = targetDirectoryPath.toString();

                Files.createDirectories(targetDirectoryPath);

                extractZip(sourceZipPath, targetDirectoryPath);
                success = true;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to extract ZIP via ExtractZipFileAction: {}", value, ex);
                failureReason = ex.getMessage();
            }

            String finalResolvedSource = (resolvedSourceZipPath != null) ? resolvedSourceZipPath : rawSourceZipPath;
            String finalResolvedTarget = (resolvedTargetDirectoryPath != null) ? resolvedTargetDirectoryPath : rawTargetDirectoryPath;
            boolean finalSuccess = success;
            String finalFailureReason = failureReason;

            MainThreadTaskExecutor.executeInMainThread(
                    () -> Listeners.ON_ZIP_EXTRACTED.onZipExtracted(finalResolvedSource, finalResolvedTarget, finalSuccess, finalFailureReason),
                    MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        });
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.extract_zip_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.extract_zip_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValuePreset() {
        return "/config/archive.zip||/config/extracted/archive_contents";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.extract_zip_file.value.source"),
                Component.translatable("fancymenu.actions.extract_zip_file.value.target"), null, callback -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (callback != null) {
                        String newValue = callback.getFirst() + "||" + callback.getSecond();
                        instance.value = newValue;
                        onEditingCompleted.accept(instance, oldValue, newValue);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                });

        String val = instance.value;
        if ((val != null) && val.contains("||")) {
            String[] array = val.split("\\|\\|", 2);
            s.setFirstText(array[0]);
            s.setSecondText(array[1]);
        }

        var opened = Dialogs.openGeneric(s, this.getDisplayName(), null, DualTextInputWindowBody.PIP_WINDOW_WIDTH, DualTextInputWindowBody.PIP_WINDOW_HEIGHT);
        opened.getSecond().addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });

    }

    private void extractZip(@NotNull Path sourceZipPath, @NotNull Path targetDirectoryPath) throws IOException {
        Path normalizedTargetDir = targetDirectoryPath.toAbsolutePath().normalize();

        try (ZipFile zipFile = new ZipFile(sourceZipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = sanitizeZipEntryName(entry.getName());
                if (entryName.isEmpty()) {
                    continue;
                }

                Path destinationPath = normalizedTargetDir.resolve(entryName).normalize();
                if (!destinationPath.startsWith(normalizedTargetDir)) {
                    throw new SecurityException("Blocked ZIP entry outside target directory: " + entry.getName());
                }

                if (entry.isDirectory() || entryName.endsWith("/")) {
                    Files.createDirectories(destinationPath);
                    continue;
                }

                Path parent = destinationPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (InputStream input = zipFile.getInputStream(entry)) {
                    Files.copy(input, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private @NotNull String sanitizeZipEntryName(@Nullable String rawName) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains(":")) {
            return "";
        }
        if (normalized.contains("\0")) {
            return "";
        }
        return normalized;
    }

    private @NotNull Path resolveAllowedPath(@NotNull String path) {
        String resolved = DotMinecraftUtils.resolveMinecraftPath(path);
        if (!DotMinecraftUtils.isInsideMinecraftDirectory(resolved)) {
            resolved = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resolved);
        }

        Path normalized = Paths.get(resolved).toAbsolutePath().normalize();
        Path minecraftDir = DotMinecraftUtils.getMinecraftDirectory().toAbsolutePath().normalize();
        Path gameDir = GameDirectoryUtils.getGameDirectory().toPath().toAbsolutePath().normalize();

        if (!normalized.startsWith(gameDir) && !normalized.startsWith(minecraftDir)) {
            throw new SecurityException("Path must stay inside the game directory or default .minecraft directory!");
        }

        return normalized;
    }

}
