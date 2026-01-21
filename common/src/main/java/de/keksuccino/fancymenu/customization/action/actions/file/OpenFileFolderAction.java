package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenFileFolderAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public OpenFileFolderAction() {
        super("open_file_folder_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        if (value == null || value.isBlank()) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: No path provided!");
            return;
        }

        Path normalizedPath;
        try {
            normalizedPath = resolveTargetPath(value.trim());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: Failed to resolve path: {}", value, ex);
            return;
        }

        if (!Files.exists(normalizedPath)) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: Path does not exist: {}", normalizedPath);
            return;
        }

        try {
            Util.getPlatform().openPath(normalizedPath);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: Failed to open path: {}", normalizedPath, ex);
        }

    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.open_file_folder");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.open_file_folder.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.open_file_folder.value");
    }

    @Override
    public String getValuePreset() {
        return "/config/fancymenu";
    }

    private @NotNull Path resolveTargetPath(@NotNull String targetPath) {
        String resolved = DotMinecraftUtils.resolveMinecraftPath(targetPath);
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
