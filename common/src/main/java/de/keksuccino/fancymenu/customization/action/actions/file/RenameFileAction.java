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
import java.nio.file.FileAlreadyExistsException;

public class RenameFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public RenameFileAction() {
        super("rename_file_in_game_dir");
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
                String filePath = valueArray[0];
                String newFileName = valueArray[1];
                
                // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
                filePath = DotMinecraftUtils.resolveMinecraftPath(filePath);
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(filePath)) {
                    filePath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(filePath);
                }
                
                File oldFile = new File(filePath);
                if (oldFile.isFile()) {
                    // Get the parent directory and construct the new file path
                    File parentDir = oldFile.getParentFile();
                    File newFile = new File(parentDir, newFileName);
                    
                    if (!newFile.exists()) {
                        Files.move(oldFile, newFile);
                    } else {
                        throw new FileAlreadyExistsException("File with the new name already exists! Can't rename to: " + newFileName);
                    }
                } else {
                    throw new FileNotFoundException("Source file not found! Can't rename: " + filePath);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to rename file in game directory via RenameFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.rename_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.rename_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValueExample() {
        return "/config/some_mod_folder/old_file_name.txt||new_file_name.txt";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {

        DualTextInputScreen s = DualTextInputScreen.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.rename_file.value.filepath"),
                Component.translatable("fancymenu.actions.rename_file.value.new_name"), null, callback -> {
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
