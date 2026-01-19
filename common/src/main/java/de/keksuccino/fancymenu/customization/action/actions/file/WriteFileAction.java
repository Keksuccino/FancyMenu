package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Consumer;

public class WriteFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";

    public WriteFileAction() {
        super("write_file_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        if (value == null || value.isEmpty()) {
            LOGGER.error("[FANCYMENU] WriteFileAction: No value provided!");
            return;
        }

        WriteFileConfig config = WriteFileConfig.parse(value);
        if (config == null) {
            LOGGER.error("[FANCYMENU] WriteFileAction: Failed to parse configuration!");
            return;
        }

        try {

            //Just in case a placeholder or something added multi-line content to the value
            value = value.replace("\n", "\\n").replace("\r", "\\n");

            // We only allow the default .minecraft directory and the instance's actual game directory for safety reasons
            String filePath = DotMinecraftUtils.resolveMinecraftPath(config.targetPath);
            if (!DotMinecraftUtils.isInsideMinecraftDirectory(filePath)) {
                filePath = GameDirectoryUtils.getAbsoluteGameDirectoryPath(filePath);
            }

            File targetFile = new File(filePath);
            
            // Create parent directories if they don't exist
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            // Convert string representation of line breaks to actual line breaks
            String contentToWrite = config.content.replace("\\n", "\n");

            if (config.appendMode) {
                // Append to file
                if (!targetFile.exists()) {
                    targetFile.createNewFile();
                }
                Files.write(targetFile.toPath(), contentToWrite.getBytes(), StandardOpenOption.APPEND);
            } else {
                // Override file (create new or replace existing)
                try (FileWriter writer = new FileWriter(targetFile, false)) {
                    writer.write(contentToWrite);
                }
            }

            LOGGER.info("[FANCYMENU] Successfully wrote to file: " + filePath);

        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to write file via WriteFileAction: " + config.targetPath, ex);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Unexpected error in WriteFileAction: " + value, ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.write_file");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.write_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display in the custom value edit screen
    }

    @Override
    public String getValueExample() {
        return "/config/mymod/output.txt|||Hello World!\\nThis is a new line.|||false";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        WriteFileActionValueScreen s = new WriteFileActionValueScreen(
                Objects.requireNonNullElse(instance.value, this.getValueExample()),
                value -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (value != null) {
                        instance.value = value;
                        onEditingCompleted.accept(instance, oldValue, value);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                    PiPWindow window = windowHolder[0];
                    if (window != null) {
                        window.close();
                    }
                });
        PiPWindow window = new PiPWindow(s.getTitle())
                .setScreen(s)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        windowHolder[0] = window;
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        window.addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    public static class WriteFileConfig {
        public String targetPath = "";
        public String content = "";
        public boolean appendMode = false;

        public String serialize() {
            return targetPath + VALUE_SEPARATOR + content + VALUE_SEPARATOR + appendMode;
        }

        @Nullable
        public static WriteFileConfig parse(String value) {
            if (value == null || value.isEmpty()) return null;

            WriteFileConfig config = new WriteFileConfig();
            String[] parts = value.split("\\|\\|\\|", -1);

            try {
                if (parts.length >= 1) config.targetPath = parts[0];
                if (parts.length >= 2) config.content = parts[1];
                if (parts.length >= 3) config.appendMode = Boolean.parseBoolean(parts[2]);
                
                return config;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to parse write file configuration!", ex);
                return null;
            }
        }
    }

    public static class WriteFileActionValueScreen extends CellScreen {

        protected WriteFileConfig config;
        protected Consumer<String> callback;

        protected WriteFileActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.write_file.edit_value"));
            this.callback = callback;
            this.config = WriteFileConfig.parse(value);
            if (this.config == null) {
                this.config = new WriteFileConfig();
            }
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            // Target File Path
            this.addLabelCell(Component.translatable("fancymenu.actions.write_file.edit.target_path"));
            TextInputCell pathCell = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.targetPath = s)
                    .setText(this.config.targetPath);
            pathCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.write_file.edit.target_path.desc")));

            this.addCellGroupEndSpacerCell();

            // Content
            this.addLabelCell(Component.translatable("fancymenu.actions.write_file.edit.content"));
            TextInputCell contentCell = this.addTextInputCell(null, true, true)
                    .setEditorMultiLineMode(true)
                    .setEditListener(s -> this.config.content = s.replace("\n", "\\n"))
                    .setText(this.config.content);
            contentCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.write_file.edit.content.desc")));

            this.addCellGroupEndSpacerCell();

            // Append Mode Toggle
            CycleButton<CommonCycles.CycleEnabledDisabled> appendModeButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.write_file.edit.mode", this.config.appendMode),
                    (value, button) -> this.config.appendMode = value.getAsBoolean());
            appendModeButton.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.write_file.edit.mode.desc")));
            this.addWidgetCell(appendModeButton, true);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.config.targetPath.isEmpty();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.callback.accept(this.config.serialize());
        }

    }

}
