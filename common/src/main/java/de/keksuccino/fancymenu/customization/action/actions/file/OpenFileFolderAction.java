package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

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

        GameDirectoryActionPathResolver.ResolvedPath target;
        try {
            GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create();
            target = this.resolveWithResolver(value.trim(), resolver);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: Failed to resolve path: {}", value, ex);
            return;
        }

        Path normalizedPath = target.path();
        try {
            Path realPath = this.resolveRealPath(target);
            Util.getPlatform().openFile(realPath.toFile());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] OpenFileFolderAction: Failed to open path: {}", normalizedPath, ex);
        }

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.open_file_folder");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.open_file_folder.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.open_file_folder.value");
    }

    @Override
    public String getValuePreset() {
        return "/config/fancymenu";
    }

    GameDirectoryActionPathResolver.ResolvedPath resolveWithResolver(@NotNull String value, @NotNull GameDirectoryActionPathResolver resolver) throws IOException {
        GameDirectoryActionPathResolver.ResolvedPath target = resolver.resolve(value);
        target.revalidate();
        if (!Files.exists(target.path(), LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Path does not exist: " + target.path());
        }
        target.revalidate();
        return target;
    }

    Path resolveRealPath(@NotNull GameDirectoryActionPathResolver.ResolvedPath target) throws IOException {
        target.revalidate();
        // Give the OS the captured real target so swapping a safe final symlink cannot redirect the open operation.
        Path realPath = target.path().toRealPath();
        target.revalidate();
        return realPath;
    }

}
