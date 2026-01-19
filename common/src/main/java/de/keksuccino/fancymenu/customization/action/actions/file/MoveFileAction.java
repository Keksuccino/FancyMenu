package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MoveFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public MoveFileAction() {
        super("move_file_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        try {
            if ((value != null) && value.contains("||")) {
                String[] valueArray = value.split("\\|\\|", 2);
                String rawSourcePath = valueArray[0];
                String rawDestinationPath = valueArray[1];
                boolean wildcardSource = isWildcardPath(rawSourcePath);
                if (isWildcardPath(rawDestinationPath)) {
                    throw new IllegalArgumentException("Destination path cannot end with '*': " + rawDestinationPath);
                }
                String sourcePath = resolveActionPath(rawSourcePath, wildcardSource);
                String destinationPath = resolveActionPath(rawDestinationPath, false);
                File sourceFile = new File(sourcePath);
                File destinationFile = new File(destinationPath);
                if (!sourceFile.exists()) {
                    throw new FileNotFoundException("Source not found! Can't move: " + (wildcardSource ? rawSourcePath : sourcePath));
                }
                if (wildcardSource) {
                    if (!sourceFile.isDirectory()) {
                        throw new FileNotFoundException("Source directory not found! Can't move: " + rawSourcePath);
                    }
                    ensureDestinationDirectory(destinationFile, destinationPath);
                    moveWildcardFiles(sourceFile, destinationFile);
                    return;
                }
                if (destinationFile.exists()) {
                    throw new FileAlreadyExistsException("Destination exists already! Can't move to: " + destinationPath);
                }
                Path normalizedSourcePath = sourceFile.toPath().toAbsolutePath().normalize();
                Path normalizedDestinationPath = destinationFile.toPath().toAbsolutePath().normalize();
                if (normalizedDestinationPath.startsWith(normalizedSourcePath)) {
                    throw new IllegalArgumentException("Destination path cannot be inside the source path: " + destinationPath);
                }
                Path destinationParent = normalizedDestinationPath.getParent();
                if (destinationParent != null) {
                    Files.createDirectories(destinationParent);
                }
                if (sourceFile.isDirectory()) {
                    Files.move(normalizedSourcePath, normalizedDestinationPath);
                } else if (sourceFile.isFile()) {
                    Files.move(normalizedSourcePath, normalizedDestinationPath);
                } else {
                    throw new FileNotFoundException("Source not found! Can't move: " + sourcePath);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to move file in game directory via MoveFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.move_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.move_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValueExample() {
        return "/config/source_directory/some_file.txt||/config/destination_directory";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.move_file.value.source"),
                Component.translatable("fancymenu.actions.move_file.value.destination"), null, callback -> {
                    if (callback != null) {
                        instance.value = callback.getKey() + "||" + callback.getValue();
                    }
                });

        String val = instance.value;
        if ((val != null) && val.contains("||")) {
            String[] array = val.split("\\|\\|", 2);
            s.setFirstText(array[0]);
            s.setSecondText(array[1]);
        }

        Dialogs.openGeneric(s, this.getActionDisplayName(), null, DualTextInputWindowBody.PIP_WINDOW_WIDTH, DualTextInputWindowBody.PIP_WINDOW_HEIGHT);

    }

    private void moveWildcardFiles(@NotNull File sourceDirectory, @NotNull File destinationDirectory) throws IOException {
        File[] filesToMove = sourceDirectory.listFiles(File::isFile);
        if (filesToMove == null) {
            throw new IOException("Failed to list files in source directory: " + sourceDirectory.getAbsolutePath());
        }
        for (File file : filesToMove) {
            File targetFile = new File(destinationDirectory, file.getName());
            if (targetFile.exists()) {
                throw new FileAlreadyExistsException("File exists at the destination path already! Can't move to: " + targetFile.getAbsolutePath());
            }
        }
        for (File file : filesToMove) {
            File targetFile = new File(destinationDirectory, file.getName());
            Files.move(file.toPath(), targetFile.toPath());
        }
    }

    private void ensureDestinationDirectory(@NotNull File destinationDirectory, @NotNull String destinationPath) throws IOException {
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory()) {
                throw new IllegalArgumentException("Destination must be a directory when using '*': " + destinationPath);
            }
        } else {
            Files.createDirectories(destinationDirectory.toPath());
        }
    }

    private @NotNull String resolveActionPath(@NotNull String path, boolean wildcard) {
        String processedPath = wildcard ? stripTrailingWildcard(path) : path;
        String resolvedPath = DotMinecraftUtils.resolveMinecraftPath(processedPath);
        if (!DotMinecraftUtils.isInsideMinecraftDirectory(resolvedPath)) {
            resolvedPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resolvedPath);
        }
        return resolvedPath;
    }

    private @NotNull String stripTrailingWildcard(@NotNull String path) {
        if (path.length() <= 1) {
            throw new IllegalArgumentException("Wildcard path requires a directory before '*': " + path);
        }
        String withoutWildcard = path.substring(0, path.length() - 1);
        if (withoutWildcard.isEmpty()) {
            throw new IllegalArgumentException("Wildcard path requires a directory before '*': " + path);
        }
        return withoutWildcard;
    }

    private boolean isWildcardPath(@Nullable String path) {
        return (path != null) && path.endsWith("*");
    }

}
