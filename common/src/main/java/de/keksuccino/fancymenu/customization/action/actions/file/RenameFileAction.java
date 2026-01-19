package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                if (newFileName.isEmpty()) {
                    throw new IllegalArgumentException("New name cannot be empty!");
                }
                String resolvedPath = DotMinecraftUtils.resolveMinecraftPath(filePath);
                if (!DotMinecraftUtils.isInsideMinecraftDirectory(resolvedPath)) {
                    resolvedPath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resolvedPath);
                }
                File oldFile = new File(resolvedPath);
                if (!oldFile.exists()) {
                    throw new FileNotFoundException("Source not found! Can't rename: " + resolvedPath);
                }
                File parentDir = oldFile.getParentFile();
                if (parentDir == null) {
                    throw new IllegalStateException("Unable to resolve parent directory for: " + resolvedPath);
                }
                File newFile = new File(parentDir, newFileName);
                if (newFile.exists()) {
                    throw new FileAlreadyExistsException("Target already exists! Can't rename to: " + newFile.getAbsolutePath());
                }
                Path sourcePath = oldFile.toPath();
                Path targetPath = newFile.toPath();
                Files.move(sourcePath, targetPath);
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
        return "/config/some_mod_folder/old_name.txt||new_name.txt";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getActionDisplayName(),
                Component.translatable("fancymenu.actions.rename_file.value.filepath"),
                Component.translatable("fancymenu.actions.rename_file.value.new_name"), null, callback -> {
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

        var opened = Dialogs.openGeneric(s, this.getActionDisplayName(), null, DualTextInputWindowBody.PIP_WINDOW_WIDTH, DualTextInputWindowBody.PIP_WINDOW_HEIGHT);
        opened.getSecond().addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });

    }

}
