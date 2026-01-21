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
            if (value != null) {
                // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
                value = DotMinecraftUtils.resolveMinecraftPath(value);
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(value)) {
                    value = GameDirectoryUtils.getAbsoluteGameDirectoryPath(value);
                }
                File f = new File(value);
                if (!f.exists()) {
                    // Create parent directories if they don't exist
                    File parentDir = f.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    // Create the file
                    f.createNewFile();
                }
                // If file already exists, do nothing and don't log
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to create file in game directory via CreateFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.create_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.create_file.desc");
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
