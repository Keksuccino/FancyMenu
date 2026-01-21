package de.keksuccino.fancymenu.customization.action.actions.file;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CopyFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public CopyFileAction() {
        super("copy_file_in_game_dir");
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
                    throw new FileNotFoundException("Source not found! Can't copy: " + (wildcardSource ? rawSourcePath : sourcePath));
                }
                if (wildcardSource) {
                    if (!sourceFile.isDirectory()) {
                        throw new FileNotFoundException("Source directory not found! Can't copy: " + rawSourcePath);
                    }
                    ensureDestinationDirectory(destinationFile, destinationPath);
                    copyWildcardFiles(sourceFile, destinationFile);
                    return;
                }
                if (destinationFile.exists()) {
                    throw new FileAlreadyExistsException("Destination exists already! Can't copy to: " + destinationPath);
                }
                Path normalizedSourcePath = sourceFile.toPath().toAbsolutePath().normalize();
                Path normalizedDestinationPath = destinationFile.toPath().toAbsolutePath().normalize();
                if (normalizedDestinationPath.startsWith(normalizedSourcePath)) {
                    throw new IllegalArgumentException("Destination path cannot be inside the source path: " + destinationPath);
                }
                Path destinationParent = normalizedDestinationPath.getParent();
                if (destinationParent != null) {
                    java.nio.file.Files.createDirectories(destinationParent);
                }
                if (sourceFile.isDirectory()) {
                    copyDirectoryRecursively(sourceFile, destinationFile);
                } else if (sourceFile.isFile()) {
                    Files.copy(sourceFile, destinationFile);
                } else {
                    throw new FileNotFoundException("Source not found! Can't copy: " + sourcePath);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to copy file in game directory via CopyFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.copy_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.copy_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValuePreset() {
        return "/config/source_directory/some_file.txt||/config/destination_directory/some_file_copy.txt";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.copy_file.value.source"),
                Component.translatable("fancymenu.actions.copy_file.value.destination"), null, callback -> {
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

    private void copyDirectoryRecursively(@NotNull File sourceDirectory, @NotNull File destinationDirectory) throws IOException {
        Path sourcePath = sourceDirectory.toPath();
        Path destinationPath = destinationDirectory.toPath();
        java.nio.file.Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = destinationPath.resolve(sourcePath.relativize(dir));
                java.nio.file.Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = destinationPath.resolve(sourcePath.relativize(file));
                java.nio.file.Files.copy(file, targetFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyWildcardFiles(@NotNull File sourceDirectory, @NotNull File destinationDirectory) throws IOException {
        File[] filesToCopy = sourceDirectory.listFiles(File::isFile);
        if (filesToCopy == null) {
            throw new IOException("Failed to list files in source directory: " + sourceDirectory.getAbsolutePath());
        }
        for (File file : filesToCopy) {
            File targetFile = new File(destinationDirectory, file.getName());
            if (targetFile.exists()) {
                throw new FileAlreadyExistsException("File exists at the destination path already! Can't copy to: " + targetFile.getAbsolutePath());
            }
        }
        for (File file : filesToCopy) {
            File targetFile = new File(destinationDirectory, file.getName());
            Files.copy(file, targetFile);
        }
    }

    private void ensureDestinationDirectory(@NotNull File destinationDirectory, @NotNull String destinationPath) throws IOException {
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory()) {
                throw new IllegalArgumentException("Destination must be a directory when using '*': " + destinationPath);
            }
        } else {
            java.nio.file.Files.createDirectories(destinationDirectory.toPath());
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
