package de.keksuccino.fancymenu.customization.action.actions.other;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.PiPCellStringBuilderWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MimicKeybindAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_DELIMITER = "|||";
    private static final long DEFAULT_KEEP_DURATION_MS = 1000L;
    private static long lastErrorNotification = -1;

    public MimicKeybindAction() {
        super("mimic_keybind");
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

        MimicKeybindConfig config = MimicKeybindConfig.parse(value);
        if (config.keybindName.isEmpty()) {
            return;
        }

        KeyMapping keyMapping = findKeyMapping(config.keybindName);
        if (keyMapping == null) {
            handleMissingKeybind(config.keybindName);
            return;
        }

        InputConstants.Key key = Services.PLATFORM.getKeyMappingKey(keyMapping);
        if (key == null) {
            LOGGER.error("[FANCYMENU] MimicKeybindAction could not resolve bound key for '{}'!", config.keybindName);
            handleMissingKeybind(config.keybindName);
            return;
        }

        if (!this.triggerKeybind(keyMapping, key, config)) {
            LOGGER.error("[FANCYMENU] MimicKeybindAction does not support key type '{}' for '{}'!", key.getType(), config.keybindName);
            handleMissingKeybind(config.keybindName);
        }

    }

    protected boolean triggerKeybind(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key, @NotNull MimicKeybindConfig config) {
        boolean keepPressed = config.keepPressed && !config.pressedDurationMs.isBlank();
        long holdDuration = keepPressed ? Math.max(config.getPressedDurationAsLong(), 1L) : 0L;

        if (key.getType() == InputConstants.Type.MOUSE) {
            pressMouseKey(keyMapping, key, keepPressed, holdDuration);
            return true;
        }

        if (key.getType() == InputConstants.Type.KEYSYM || key.getType() == InputConstants.Type.SCANCODE) {
            return pressKeyboardKey(keyMapping, key, keepPressed, holdDuration);
        }

        return false;
    }

    private void pressMouseKey(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key, boolean keepPressed, long holdDurationMs) {
        KeyMapping.set(key, true);
        KeyMapping.click(key);
        if (keepPressed) {
            startHoldThread(keyMapping, key, InputConstants.UNKNOWN.getValue(), -1, false, holdDurationMs);
        } else {
            MainThreadTaskExecutor.executeInMainThread(() -> KeyMapping.set(key, false), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }
    }

    private boolean pressKeyboardKey(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key, boolean keepPressed, long holdDurationMs) {
        Minecraft minecraft = Minecraft.getInstance();
        KeyboardHandler handler = minecraft.keyboardHandler;

        long window = minecraft.getWindow().getWindow();
        int keyCode;
        int scanCode;
        if (key.getType() == InputConstants.Type.KEYSYM) {
            keyCode = key.getValue();
            scanCode = GLFW.glfwGetKeyScancode(keyCode);
        } else {
            keyCode = InputConstants.UNKNOWN.getValue();
            scanCode = key.getValue();
        }

        KeyMapping.set(key, true);
        handler.keyPress(window, keyCode, scanCode, 1, 0);

        if (keepPressed) {
            startHoldThread(keyMapping, key, keyCode, scanCode, true, holdDurationMs);
        } else {
            MainThreadTaskExecutor.executeInMainThread(() -> releaseKeyboardKey(key, keyCode, scanCode), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }
        return true;
    }

    private void releaseKeyboardKey(@NotNull InputConstants.Key key, int keyCode, int scanCode) {
        Minecraft minecraft = Minecraft.getInstance();
        KeyboardHandler handler = minecraft.keyboardHandler;
        handler.keyPress(minecraft.getWindow().getWindow(), keyCode, scanCode, 0, 0);
        KeyMapping.set(key, false);
    }

    private void startHoldThread(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key, int keyCode, int scanCode, boolean keyboard, long durationMs) {
        Thread holdThread = new Thread(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            long start = System.currentTimeMillis();
            while (minecraft.isRunning() && !Thread.currentThread().isInterrupted() && (System.currentTimeMillis() - start) < durationMs) {
                if (!keyMapping.isDown()) {
                    minecraft.execute(() -> KeyMapping.set(key, true));
                }
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            minecraft.execute(() -> {
                if (keyboard) {
                    releaseKeyboardKey(key, keyCode, scanCode);
                } else {
                    KeyMapping.set(key, false);
                }
            });
        }, "FancyMenu-MimicKeybindHold-" + keyMapping.getName());
        holdThread.start();
    }

    @Nullable
    protected KeyMapping findKeyMapping(@NotNull String identifier) {
        String trimmedId = identifier.trim();
        Minecraft minecraft = Minecraft.getInstance();
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (keyMapping.getName().equals(trimmedId)) {
                return keyMapping;
            }
        }
        return null;
    }

    protected void handleMissingKeybind(@NotNull String identifier) {
        LOGGER.error("[FANCYMENU] MimicKeybindAction failed to find keybind '{}'!", identifier);
        long now = System.currentTimeMillis();
        if ((lastErrorNotification + 60000) < now) {
            lastErrorNotification = now;
            Dialogs.openMessage(Component.translatable("fancymenu.actions.mimic_keybind.error"), MessageDialogStyle.ERROR);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.mimic_keybind");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.mimic_keybind.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValuePreset() {
        return "key.jump|||false|||1000";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        MimicKeybindActionValueScreen screen = new MimicKeybindActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValuePreset()), value -> {
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

    @NotNull
    protected static List<String> getAvailableKeybindNames() {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            names.add(keyMapping.getName());
        }
        if (names.isEmpty()) {
            names.add("[NO KEYBINDS FOUND]");
        }
        return names;
    }

    public static class MimicKeybindActionValueScreen extends PiPCellStringBuilderWindowBody {

        protected MimicKeybindConfig config;
        protected EditBoxSuggestions suggestions;
        @Nullable
        protected LabelCell keyInfoLabel;

        protected MimicKeybindActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.mimic_keybind.edit.title"), callback);
            this.config = MimicKeybindConfig.parse(value);
        }

        @Override
        protected void initCells() {
            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.keybind"));
            TextInputCell keybindInput = this.addTextInputCell(null, true, true).setText(this.config.keybindName);

            this.suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, keybindInput.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, getAvailableKeybindNames());
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            keybindInput.editBox.setResponder(text -> {
                this.config.keybindName = text;
                this.suggestions.updateCommandInfo();
                if (this.keyInfoLabel != null) {
                    this.keyInfoLabel.setText(this.buildCurrentKeyInfoComponent());
                }
            });

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.keep_pressed.label"));
            WidgetCell keepPressedCell = this.addCycleButtonCell(
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.mimic_keybind.edit.keep_pressed", this.config.keepPressed),
                    true,
                    (value, button) -> {
                        this.config.keepPressed = value.getAsBoolean();
                        updateDurationFieldState();
                    });
            @SuppressWarnings("unchecked")
            CycleButton<CommonCycles.CycleEnabledDisabled> keepPressedButton =
                    (CycleButton<CommonCycles.CycleEnabledDisabled>) keepPressedCell.widget;
            keepPressedButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimic_keybind.edit.keep_pressed.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.pressed_duration"));
            TextInputCell durationInput = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.pressedDurationMs = s)
                    .setText(this.config.pressedDurationMs);
            durationInput.editBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("fancymenu.actions.mimic_keybind.edit.pressed_duration.desc")));
            durationInput.editBox.moveCursorToStart(true);
            this.durationCell = durationInput;
            updateDurationFieldState();

            this.addCellGroupEndSpacerCell();

            this.keyInfoLabel = this.addLabelCell(this.buildCurrentKeyInfoComponent());

            this.addSpacerCell(20);
        }

        private TextInputCell durationCell;

        private void updateDurationFieldState() {
            if (this.durationCell != null) {
                this.durationCell.setText(this.config.pressedDurationMs);
                this.durationCell.editBox.setEditable(this.config.keepPressed);
                if (!this.config.keepPressed && this.durationCell.editBox.isFocused()) {
                    this.durationCell.editBox.setFocused(false);
                }
            }
        }

        @NotNull
        protected Component buildCurrentKeyInfoComponent() {
            KeyMapping keyMapping = getKeyMapping(this.config.keybindName);
            if (keyMapping == null) {
                return Component.translatable("fancymenu.actions.mimic_keybind.edit.keybind_missing");
            }
            Component keyName = Component.translatable(keyMapping.getName());
            Component keyValue = keyMapping.getTranslatedKeyMessage();
            return Component.translatable("fancymenu.actions.mimic_keybind.edit.keybind_preview", keyName, keyValue);
        }

        @Nullable
        protected KeyMapping getKeyMapping(@NotNull String identifier) {
            Minecraft minecraft = Minecraft.getInstance();
            for (KeyMapping keyMapping : minecraft.options.keyMappings) {
                if (keyMapping.getName().equals(identifier.trim())) {
                    return keyMapping;
                }
            }
            return null;
        }

        @Override
        public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.suggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.suggestions.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            if (this.suggestions.mouseScrolled(scrollDeltaY)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.suggestions.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean allowDone() {
            return !this.config.keybindName.trim().isEmpty() && (!this.config.keepPressed || !this.config.pressedDurationMs.isBlank());
        }

        @Override
        public @NotNull String buildString() {
            return this.config.serialize();
        }

        @Override
        protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        }

    }

    protected static class MimicKeybindConfig {

        protected String keybindName = "";
        protected boolean keepPressed = false;
        @NotNull
        protected String pressedDurationMs = "" + DEFAULT_KEEP_DURATION_MS;

        protected static MimicKeybindConfig parse(@Nullable String rawValue) {
            MimicKeybindConfig config = new MimicKeybindConfig();
            if (rawValue == null || rawValue.isEmpty()) {
                return config;
            }

            String[] parts = rawValue.split("\\Q" + VALUE_DELIMITER + "\\E", -1);
            config.keybindName = parts[0].trim();
            if (parts.length > 1) {
                config.keepPressed = Boolean.parseBoolean(parts[1].trim());
            }
            if (parts.length > 2) {
                config.pressedDurationMs = parts[2];
            }
            return config;
        }

        public long getPressedDurationAsLong() {
            if (MathUtils.isLong(this.pressedDurationMs.trim())) {
                return Long.parseLong(this.pressedDurationMs.trim());
            }
            return 0L;
        }

        protected String serialize() {
            String sanitizedName = this.keybindName.trim();
            if (sanitizedName.isEmpty()) {
                sanitizedName = "";
            }
            return sanitizedName + VALUE_DELIMITER + this.keepPressed + VALUE_DELIMITER + this.pressedDurationMs;
        }

    }

}

