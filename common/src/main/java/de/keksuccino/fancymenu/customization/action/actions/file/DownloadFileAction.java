package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputScreen;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
        if ((value != null) && value.contains("||")) {
            String[] valueArray = value.split("\\|\\|", 2);
            String fileUrl = valueArray[0];
            String targetPath = valueArray[1];
            
            // Start the download asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    downloadFile(fileUrl, targetPath);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to download file via DownloadFileAction: " + value, ex);
                    MainThreadTaskExecutor.executeInMainThread(() -> Listeners.ON_FILE_DOWNLOADED.onFileDownloaded(fileUrl, targetPath, false), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            });
        }
    }

    private void downloadFile(String fileUrl, String targetPath) throws Exception {

        // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
        targetPath = DotMinecraftUtils.resolveMinecraftPath(targetPath);
        if (!DotMinecraftUtils.isInsideMinecraftDirectory(targetPath)) {
            targetPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(targetPath);
        }
        
        File targetFile = new File(targetPath);
        
        // Create parent directories if they don't exist
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Check if file already exists
        if (targetFile.exists()) {
            LOGGER.warn("[FANCYMENU] Target file already exists, overwriting via DownloadFileAction: " + targetPath);
        }
        
        // Open connection and download the file
        InputStream inputStream = WebUtils.openResourceStream(fileUrl);
        if (inputStream == null) {
            throw new Exception("Failed to open connection to URL in DownloadFileAction: " + fileUrl);
        }
        
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            
            LOGGER.info("[FANCYMENU] Successfully downloaded file via the DownloadFileAction: {} ({} bytes)", targetPath, totalBytesRead);

            String finalTargetPath = targetPath;
            MainThreadTaskExecutor.executeInMainThread(() -> Listeners.ON_FILE_DOWNLOADED.onFileDownloaded(fileUrl, finalTargetPath, true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

        } finally {
            CloseableUtils.closeQuietly(inputStream);
        }

    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.download_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.download_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValueExample() {
        return "https://example.com/file.txt||/config/downloaded_file.txt";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {

        DualTextInputScreen s = DualTextInputScreen.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.download_file.value.url"),
                Component.translatable("fancymenu.actions.download_file.value.target_path"), null, callback -> {
                    if (callback != null) {
                        instance.value = callback.getKey() + "||" + callback.getValue();
                    }
                    Minecraft.getInstance().setScreen(parentScreen);
                });

        String val = instance.value;
        if ((val != null) && val.contains("||")) {
            String[] array = val.split("\\|\\|", 2);
            s.setFirstText(array[0]);
            s.setSecondText(array[1]);
        }

        Minecraft.getInstance().setScreen(s);

    }

}
