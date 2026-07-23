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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class DownloadFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for file writing

    public DownloadFileAction() {
        super("download_file_to_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        try {
            this.executeWithResolver(value, GameDirectoryActionPathResolver.create());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to initialize DownloadFileAction: " + value, ex);
        }
    }

    CompletableFuture<Void> executeWithResolver(@Nullable String value, @NotNull GameDirectoryActionPathResolver resolver) {
        if ((value == null) || !value.contains("||")) {
            return CompletableFuture.completedFuture(null);
        }
        String[] valueArray = value.split("\\|\\|", 2);
        String fileUrl = valueArray[0];
        String targetDirectoryPath = valueArray[1];
        return CompletableFuture.runAsync(() -> {
            GameDirectoryActionPathResolver.ResolvedPath targetDirectory = null;
            try {
                targetDirectory = this.resolveActionDirectoryPath(targetDirectoryPath, resolver);
                String finalTargetPath = this.downloadFile(fileUrl, targetDirectory);
                RevisionSafeListenerDispatch.scheduleIfActive(Listeners.ON_FILE_DOWNLOADED, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK), () -> Listeners.ON_FILE_DOWNLOADED.onFileDownloaded(fileUrl, finalTargetPath, true));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to download file via DownloadFileAction: " + value, ex);
                String failurePath = (targetDirectory != null) ? targetDirectory.path().toString() : targetDirectoryPath;
                RevisionSafeListenerDispatch.scheduleIfActive(Listeners.ON_FILE_DOWNLOADED, task -> MainThreadTaskExecutor.executeInMainThread(task, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK), () -> Listeners.ON_FILE_DOWNLOADED.onFileDownloaded(fileUrl, failurePath, false));
            }
        });
    }

    @NotNull String downloadFile(@NotNull String fileUrl, @NotNull GameDirectoryActionPathResolver.ResolvedPath targetDirectory) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL actualURL = new URL(fileUrl);
            connection = (HttpURLConnection) actualURL.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/4.0");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                throw new IOException("Server returned response code " + responseCode + " for URL: " + fileUrl);
            }

            String fileName = resolveFileName(connection, actualURL);
            targetDirectory.revalidate();
            GameDirectoryActionPathResolver.ResolvedPath targetFile = targetDirectory.resolveSingleComponentChild(fileName);
            Path targetPath = targetFile.path();
            if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                LOGGER.warn("[FANCYMENU] Target file already exists, overwriting via DownloadFileAction: " + targetPath);
            }

            Path temporaryPath = null;
            GameDirectoryActionPathResolver.ResolvedPath temporaryFile = null;
            try (InputStream inputStream = connection.getInputStream()) {
                targetDirectory.revalidate();
                targetFile.revalidate();
                temporaryPath = Files.createTempFile(targetDirectory.path(), ".fancymenu-download-", ".tmp");
                temporaryFile = targetDirectory.resolveSingleComponentChild(temporaryPath.getFileName().toString());
                long totalBytesRead = 0;
                try (OutputStream outputStream = Files.newOutputStream(temporaryPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }
                }
                targetDirectory.revalidate();
                temporaryFile.revalidate();
                targetFile.revalidate();
                Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                temporaryPath = null;
                LOGGER.info("[FANCYMENU] Successfully downloaded file via the DownloadFileAction: {} ({} bytes)", targetPath, totalBytesRead);
                return targetPath.toString();
            } finally {
                if ((temporaryPath != null) && (temporaryFile != null)) {
                    this.cleanupTemporaryFile(temporaryFile);
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    boolean cleanupTemporaryFile(@NotNull GameDirectoryActionPathResolver.ResolvedPath temporaryFile) {
        try {
            temporaryFile.revalidate();
            return Files.deleteIfExists(temporaryFile.path());
        } catch (Exception cleanupException) {
            // A leaked temporary file is safer than deleting the same lexical name after an ancestor or root was redirected.
            LOGGER.warn("[FANCYMENU] Skipped unsafe temporary download cleanup: " + temporaryFile.path(), cleanupException);
            return false;
        }
    }

    private @NotNull GameDirectoryActionPathResolver.ResolvedPath resolveActionDirectoryPath(@NotNull String path, @NotNull GameDirectoryActionPathResolver resolver) throws IOException {
        GameDirectoryActionPathResolver.ResolvedPath directory = resolver.resolve(path);
        Path directoryPath = directory.path();
        directory.revalidate();
        if (Files.exists(directoryPath, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(directoryPath)) {
                throw new IllegalArgumentException("Target path must be a directory: " + directoryPath);
            }
        } else {
            Files.createDirectories(directoryPath);
        }
        directory.revalidate();
        return directory;
    }

    private @NotNull String resolveFileName(@NotNull HttpURLConnection connection, @NotNull URL url) {
        String fileName = extractFileNameFromContentDisposition(connection.getHeaderField("Content-Disposition"));
        if ((fileName == null) || fileName.isEmpty()) {
            fileName = extractFileNameFromUrl(url);
        }
        fileName = sanitizeFileName(fileName);
        if (fileName.isEmpty()) {
            fileName = "download_" + System.currentTimeMillis();
        }
        return fileName;
    }

    private @Nullable String extractFileNameFromContentDisposition(@Nullable String contentDisposition) {
        if ((contentDisposition == null) || contentDisposition.isEmpty()) {
            return null;
        }
        String[] segments = contentDisposition.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("filename*=")) {
                String value = trimmed.substring("filename*=".length());
                int charsetSeparator = value.indexOf("''");
                if (charsetSeparator >= 0) {
                    value = value.substring(charsetSeparator + 2);
                }
                return decodeFileName(value);
            }
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("filename=")) {
                String value = trimmed.substring("filename=".length());
                return decodeFileName(value);
            }
        }
        return null;
    }

    private @Nullable String extractFileNameFromUrl(@NotNull URL url) {
        String path = url.getPath();
        if ((path == null) || path.isEmpty() || path.endsWith("/")) {
            return null;
        }
        try {
            Path urlPath = Paths.get(path);
            Path fileName = urlPath.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        } catch (InvalidPathException ignored) {
        }
        int lastSlash = path.lastIndexOf('/') + 1;
        if ((lastSlash >= 0) && (lastSlash < path.length())) {
            return path.substring(lastSlash);
        }
        return null;
    }

    private @NotNull String sanitizeFileName(@Nullable String fileName) {
        if (fileName == null) {
            return "";
        }
        String sanitized = fileName.trim();
        if (sanitized.startsWith("\"") && sanitized.endsWith("\"") && sanitized.length() >= 2) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }
        sanitized = sanitized.replace("\\", "/");
        int lastSlash = sanitized.lastIndexOf('/') + 1;
        if ((lastSlash >= 0) && (lastSlash < sanitized.length())) {
            sanitized = sanitized.substring(lastSlash);
        }
        sanitized = sanitized.replaceAll("[\\r\\n]", "");
        sanitized = sanitized.replaceAll("[<>:\\|?*]", "_");
        sanitized = sanitized.trim();
        if (sanitized.isEmpty()) {
            return "";
        }
        try {
            return Paths.get(sanitized).getFileName().toString();
        } catch (InvalidPathException ignored) {
            return sanitized;
        }
    }

    private @NotNull String decodeFileName(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            return URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return trimmed;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.download_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.download_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValuePreset() {
        return "https://example.com/file.txt||/config/downloads";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.download_file.value.url"),
                Component.translatable("fancymenu.actions.download_file.value.target_path"), null, callback -> {
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

}
