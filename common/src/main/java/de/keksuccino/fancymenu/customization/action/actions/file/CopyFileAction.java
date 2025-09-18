package de.keksuccino.fancymenu.customization.action.actions.file;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputScreen;
import net.minecraft.client.Minecraft;
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
                // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
                String sourcePath = DotMinecraftUtils.resolveMinecraftPath(valueArray[0]);
                String destinationPath = DotMinecraftUtils.resolveMinecraftPath(valueArray[1]);
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(sourcePath)) {
                    sourcePath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(sourcePath);
                }
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(destinationPath)) {
                    destinationPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(destinationPath);
                }
                File sourceFile = new File(sourcePath);
                File destinationFile = new File(destinationPath);
                if (!sourceFile.exists()) {
                    throw new FileNotFoundException("Source not found! Can't copy: " + sourcePath);
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
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.copy_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.copy_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValueExample() {
        return "/config/source_directory||/config/destination_directory";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {

        DualTextInputScreen s = DualTextInputScreen.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.copy_file.value.source"),
                Component.translatable("fancymenu.actions.copy_file.value.destination"), null, callback -> {
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

}