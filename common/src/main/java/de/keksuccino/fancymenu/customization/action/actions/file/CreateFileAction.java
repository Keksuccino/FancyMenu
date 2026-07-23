package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class CreateFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public CreateFileAction() {
        super("create_file_in_game_dir");
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
            LOGGER.error("[FANCYMENU] Failed to create file in game directory via CreateFileAction: " + value, ex);
        }
    }

    void executeWithResolver(@Nullable String value, @NotNull GameDirectoryActionPathResolver resolver) throws IOException {
        if (value == null) {
            return;
        }
        GameDirectoryActionPathResolver.ResolvedPath target = resolver.resolve(value).requireDescendant();
        Path targetPath = target.path();
        target.revalidate();
        if (!Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
            Path parent = targetPath.getParent();
            if (parent != null) {
                target.revalidate();
                Files.createDirectories(parent);
            }
            target.revalidate();
            Files.createFile(targetPath);
        }
        // If file already exists, do nothing and don't log
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.create_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.create_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.create_file.value");
    }

    @Override
    public String getValuePreset() {
        return "/config/some_mod_folder/new_file.txt";
    }

}
