package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.listener.RevisionSafeListenerDispatch;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

        try {
            this.executeWithResolver(value, GameDirectoryActionPathResolver.create());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to initialize ExtractZipFileAction: {}", value, ex);
        }
    }

    CompletableFuture<Void> executeWithResolver(@Nullable String value, @NotNull GameDirectoryActionPathResolver resolver) {
        if ((value == null) || !value.contains("||")) {
            return CompletableFuture.completedFuture(null);
        }
        String[] valueArray = value.split("\\|\\|", 2);
        String rawSourceZipPath = valueArray[0];
        String rawTargetDirectoryPath = valueArray[1];
        return CompletableFuture.runAsync(() -> {
            GameDirectoryActionPathResolver.ResolvedPath sourceZip = null;
            GameDirectoryActionPathResolver.ResolvedPath targetDirectory = null;
            boolean success = false;
            String failureReason = null;
            try {
                sourceZip = resolver.resolve(rawSourceZipPath);
                targetDirectory = resolver.resolve(rawTargetDirectoryPath);
                sourceZip.revalidate();
                targetDirectory.revalidate();
                if (!Files.exists(sourceZip.path(), LinkOption.NOFOLLOW_LINKS) || Files.isDirectory(sourceZip.path())) {
                    throw new IOException("Source ZIP file not found or is a directory: " + rawSourceZipPath);
                }
                this.extractZip(sourceZip, targetDirectory);
                success = true;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to extract ZIP via ExtractZipFileAction: {}", value, ex);
                failureReason = ex.getMessage();
            }

            String finalResolvedSource = (sourceZip != null) ? sourceZip.path().toString() : rawSourceZipPath;
            String finalResolvedTarget = (targetDirectory != null) ? targetDirectory.path().toString() : rawTargetDirectoryPath;
            boolean finalSuccess = success;
            String finalFailureReason = failureReason;
            RevisionSafeListenerDispatch.scheduleIfActive(Listeners.ON_ZIP_EXTRACTED, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK), () -> Listeners.ON_ZIP_EXTRACTED.onZipExtracted(finalResolvedSource, finalResolvedTarget, finalSuccess, finalFailureReason));
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

    void extractZip(@NotNull GameDirectoryActionPathResolver.ResolvedPath sourceZip, @NotNull GameDirectoryActionPathResolver.ResolvedPath targetDirectory) throws IOException {
        sourceZip.revalidate();
        targetDirectory.revalidate();
        Path realSourceZipPath = sourceZip.path().toRealPath();
        sourceZip.revalidate();
        try (ZipFile zipFile = new ZipFile(realSourceZipPath.toFile())) {
            // Resolve every ZIP entry before creating the target so ZIP slip or an existing escaping link cannot leave partial output.
            List<ExtractionEntry> extractionPlan = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = sanitizeZipEntryName(entry.getName());
                if (entryName.isEmpty()) {
                    continue;
                }

                GameDirectoryActionPathResolver.ResolvedPath destination = targetDirectory.resolveRelativeChild(entryName);
                destination.revalidate();
                extractionPlan.add(new ExtractionEntry(entry, destination, entry.isDirectory() || entryName.endsWith("/")));
            }
            sourceZip.revalidate();
            targetDirectory.revalidate();
            for (ExtractionEntry extractionEntry : extractionPlan) {
                extractionEntry.destination().revalidate();
            }
            Files.createDirectories(targetDirectory.path());
            targetDirectory.revalidate();

            for (ExtractionEntry extractionEntry : extractionPlan) {
                GameDirectoryActionPathResolver.ResolvedPath destination = extractionEntry.destination();
                destination.revalidate();
                if (extractionEntry.directory()) {
                    Files.createDirectories(destination.path());
                    destination.revalidate();
                    continue;
                }

                Path parent = destination.path().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                sourceZip.revalidate();
                destination.revalidate();
                try (InputStream input = zipFile.getInputStream(extractionEntry.entry())) {
                    destination.revalidate();
                    try (OutputStream output = Files.newOutputStream(destination.path(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS)) {
                        input.transferTo(output);
                    }
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

    private record ExtractionEntry(ZipEntry entry, GameDirectoryActionPathResolver.ResolvedPath destination, boolean directory) {
    }

}
