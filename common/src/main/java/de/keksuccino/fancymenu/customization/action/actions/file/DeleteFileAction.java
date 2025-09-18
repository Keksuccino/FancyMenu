package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DeleteFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public DeleteFileAction() {
        super("delete_file_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        try {
            if ((value != null) && !value.isEmpty()) {
                boolean wildcardTarget = isWildcardPath(value);
                String resolvedPath = resolveActionPath(value, wildcardTarget);
                File targetFile = new File(resolvedPath);
                if (!targetFile.exists()) {
                    throw new FileNotFoundException("Target not found! Can't delete: " + (wildcardTarget ? value : resolvedPath));
                }
                if (wildcardTarget) {
                    if (!targetFile.isDirectory()) {
                        throw new FileNotFoundException("Target directory not found! Can't delete: " + value);
                    }
                    deleteWildcardFiles(targetFile);
                    return;
                }
                if (targetFile.isDirectory()) {
                    deleteDirectoryRecursively(targetFile);
                } else if (targetFile.isFile()) {
                    Files.delete(targetFile.toPath());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to delete file in game directory via DeleteFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.delete_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.delete_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.delete_file.value");
    }

    @Override
    public String getValueExample() {
        return "/config/some_mod_folder/*";
    }

    private void deleteDirectoryRecursively(@NotNull File directory) throws IOException {
        Path directoryPath = directory.toPath();
        Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteWildcardFiles(@NotNull File directory) throws IOException {
        File[] filesToDelete = directory.listFiles(File::isFile);
        if (filesToDelete == null) {
            throw new IOException("Failed to list files in target directory: " + directory.getAbsolutePath());
        }
        for (File file : filesToDelete) {
            Files.delete(file.toPath());
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