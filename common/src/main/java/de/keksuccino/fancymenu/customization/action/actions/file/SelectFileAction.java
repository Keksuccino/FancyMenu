package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.file.DotMinecraftUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class SelectFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";
    private static final String LIST_SEPARATOR_REGEX = "[;,]";
    private static final String DEFAULT_FILTER_DESCRIPTION = "All Files (*.*)";

    public SelectFileAction() {
        super("select_file_to_game_dir");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        SelectFileConfig config = SelectFileConfig.parse(value);
        if (config == null) {
            LOGGER.error("[FANCYMENU] SelectFileAction: Failed to parse configuration!");
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(null, null, false, false, "invalid_configuration");
            return;
        }
        if (!config.hasValidTargetPath()) {
            LOGGER.error("[FANCYMENU] SelectFileAction: No target path configured!");
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(null, config.targetPath, false, false, "missing_target_path");
            return;
        }

        Path targetPath;
        try {
            targetPath = resolveTargetPath(config.targetPath);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] SelectFileAction: Failed to resolve target path '{}'", config.targetPath, ex);
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(null, config.targetPath, false, false, ex.getMessage());
            return;
        }

        String dialogTitle = Component.translatable("fancymenu.actions.select_file.dialog_title").getString();
        if (dialogTitle.isBlank()) {
            dialogTitle = this.getDisplayName().getString();
        }

        List<String> filterPatterns = config.buildFilterPatterns();
        String filterDescription = config.getEffectiveFilterDescription();
        String selectedFilePath;

        if (filterPatterns.isEmpty()) {
            selectedFilePath = TinyFileDialogs.tinyfd_openFileDialog(dialogTitle, null, null, filterDescription, false);
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterBuffer = stack.mallocPointer(filterPatterns.size());
                for (String pattern : filterPatterns) {
                    filterBuffer.put(stack.UTF8(pattern));
                }
                filterBuffer.flip();
                selectedFilePath = TinyFileDialogs.tinyfd_openFileDialog(dialogTitle, null, filterBuffer, filterDescription, false);
            }
        }

        if (selectedFilePath == null) {
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(null, targetPath.toString(), false, true, null);
            return;
        }

        Path sourcePath;
        try {
            sourcePath = Paths.get(selectedFilePath).toAbsolutePath().normalize();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] SelectFileAction: Invalid source path returned from dialog: {}", selectedFilePath, ex);
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(selectedFilePath, targetPath.toString(), false, false, "invalid_source_path");
            return;
        }

        if (!Files.exists(sourcePath) || Files.isDirectory(sourcePath)) {
            LOGGER.error("[FANCYMENU] SelectFileAction: Source path does not point to a readable file: {}", sourcePath);
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(sourcePath.toString(), targetPath.toString(), false, false, "source_not_file");
            return;
        }

        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!config.overwriteExisting && Files.exists(targetPath)) {
                throw new FileAlreadyExistsException("Destination exists already: " + targetPath);
            }

            if (config.overwriteExisting) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(sourcePath, targetPath);
            }

            LOGGER.info("[FANCYMENU] SelectFileAction: Copied '{}' to '{}'", sourcePath, targetPath);
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(sourcePath.toString(), targetPath.toString(), true, false, null);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] SelectFileAction: Failed to copy '{}' to '{}'", sourcePath, targetPath, ex);
            Listeners.ON_FILE_SELECTED.onFileSelectionResult(sourcePath.toString(), targetPath.toString(), false, false, ex.getMessage());
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.select_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.select_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValuePreset() {
        return "/config/fancymenu/assets/background.png|||Image Files (*.png;*.jpg)|||png;jpg|||true";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        SelectFileActionValueScreen screen = new SelectFileActionValueScreen(
                Objects.requireNonNullElse(instance.value, this.getValuePreset()),
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
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
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

    private @NotNull Path resolveTargetPath(@NotNull String targetPath) throws IOException {
        String resolved = DotMinecraftUtils.resolveMinecraftPath(targetPath);
        if (!DotMinecraftUtils.isInsideMinecraftDirectory(resolved)) {
            resolved = GameDirectoryUtils.getAbsoluteGameDirectoryPath(resolved);
        }

        Path normalized = Paths.get(resolved).toAbsolutePath().normalize();
        Path minecraftDir = DotMinecraftUtils.getMinecraftDirectory().toAbsolutePath().normalize();
        Path gameDir = GameDirectoryUtils.getGameDirectory().toPath().toAbsolutePath().normalize();

        if (!normalized.startsWith(gameDir) && !normalized.startsWith(minecraftDir)) {
            throw new SecurityException("Target path must stay inside the game directory or default .minecraft directory!");
        }

        return normalized;
    }

    public static class SelectFileConfig {

        public String targetPath = "";
        public String filterDescription = DEFAULT_FILTER_DESCRIPTION;
        public String extensionsRaw = "";
        public boolean overwriteExisting = true;

        public boolean hasValidTargetPath() {
            return (this.targetPath != null) && !this.targetPath.isBlank();
        }

        @Nullable
        public static SelectFileConfig parse(@Nullable String value) {
            SelectFileConfig config = new SelectFileConfig();
            if (value == null || value.isEmpty()) {
                return config;
            }

            String[] parts = value.split("\\Q" + VALUE_SEPARATOR + "\\E", -1);

            try {
                if (parts.length >= 1) config.targetPath = parts[0];
                if (parts.length >= 2) config.filterDescription = parts[1].isEmpty() ? DEFAULT_FILTER_DESCRIPTION : parts[1];
                if (parts.length >= 3) config.extensionsRaw = parts[2];
                if (parts.length >= 4) config.overwriteExisting = Boolean.parseBoolean(parts[3]);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] SelectFileAction: Failed to parse configuration string: {}", value, ex);
                return new SelectFileConfig();
            }

            return config;
        }

        public @NotNull String serialize() {
            return String.join(VALUE_SEPARATOR,
                    Objects.toString(this.targetPath, ""),
                    Objects.toString(this.filterDescription, ""),
                    Objects.toString(this.extensionsRaw, ""),
                    Boolean.toString(this.overwriteExisting));
        }

        public @NotNull List<String> buildFilterPatterns() {
            List<String> patterns = new ArrayList<>();
            if (this.extensionsRaw == null) {
                return patterns;
            }

            String raw = this.extensionsRaw.trim();
            if (raw.isEmpty()) {
                return patterns;
            }

            String[] splitted = raw.split(LIST_SEPARATOR_REGEX);
            for (String ext : splitted) {
                String trimmed = ext.trim();
                if (trimmed.isEmpty()) continue;

                String normalized = normalizeExtension(trimmed);
                if (normalized.equals("*") || normalized.equals("*.*")) {
                    patterns.clear();
                    return patterns;
                }
                patterns.add(normalized);
            }
            return patterns;
        }

        @Nullable
        public String getEffectiveFilterDescription() {
            return (this.filterDescription == null || this.filterDescription.isBlank()) ? null : this.filterDescription;
        }

        private @NotNull String normalizeExtension(@NotNull String input) {
            String normalized = input.trim();
            if (normalized.isEmpty()) {
                return "*.*";
            }
            if (normalized.equals("*") || normalized.equals("*.*")) {
                return "*.*";
            }
            if (normalized.indexOf('*') >= 0 || normalized.indexOf('?') >= 0) {
                return normalized;
            }
            if (normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            normalized = normalized.toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return "*.*";
            }
            return "*." + normalized;
        }
    }

    public static class SelectFileActionValueScreen extends CellScreen {

        protected SelectFileConfig config;
        protected final Consumer<String> callback;

        protected SelectFileActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.select_file.edit_value"));
            this.callback = callback;
            SelectFileConfig parsed = SelectFileConfig.parse(value);
            this.config = Objects.requireNonNullElseGet(parsed, SelectFileConfig::new);
        }

        @Override
        protected void initCells() {
            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.select_file.edit.target_path"));
            TextInputCell targetPathCell = this.addTextInputCell(null, true, true)
                    .setEditListener(text -> this.config.targetPath = text)
                    .setText(this.config.targetPath);
            targetPathCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.select_file.edit.target_path.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.select_file.edit.filter_description"));
            TextInputCell filterDescriptionCell = this.addTextInputCell(null, true, true)
                    .setEditListener(text -> this.config.filterDescription = text)
                    .setText(this.config.filterDescription);
            filterDescriptionCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.select_file.edit.filter_description.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.select_file.edit.extensions"));
            TextInputCell extensionsCell = this.addTextInputCell(null, true, true)
                    .setEditListener(text -> this.config.extensionsRaw = text)
                    .setText(this.config.extensionsRaw);
            extensionsCell.editBox.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.select_file.edit.extensions.desc")));

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> overwriteButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.select_file.edit.overwrite", this.config.overwriteExisting),
                    (cycleValue, button) -> this.config.overwriteExisting = cycleValue.getAsBoolean());
            overwriteButton.setTooltip(Tooltip.create(Component.translatable("fancymenu.actions.select_file.edit.overwrite.desc")));
            this.addWidgetCell(overwriteButton, true);

            this.addStartEndSpacerCell();
        }

        @Override
        public boolean allowDone() {
            return this.config.hasValidTargetPath();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            this.callback.accept(this.config.serialize());
        }

        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }
    }
}
