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
                // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
                String oldPath = DotMinecraftUtils.resolveMinecraftPath(valueArray[0]);
                String newPath = DotMinecraftUtils.resolveMinecraftPath(valueArray[1]);
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(oldPath)) {
                    oldPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(oldPath);
                }
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(newPath)) {
                    newPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(newPath);
                }
                File oldFile = new File(oldPath);
                File newFile = new File(newPath);
                if (oldFile.isFile()) {
                    if (!newFile.isFile()) {
                        Files.move(oldFile, newFile);
                    } else {
                        throw new FileAlreadyExistsException("File exists at the destination path already! Can't move to: " + newPath);
                    }
                } else {
                    throw new FileNotFoundException("Source file not found! Can't move: " + oldPath);
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
        return "/config/old_directory/file.txt||/config/new_directory/file.txt";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {

        DualTextInputScreen s = DualTextInputScreen.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.move_file.value.source"),
                Component.translatable("fancymenu.actions.move_file.value.destination"), null, callback -> {
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
