package de.keksuccino.fancymenu.customization.action.actions.other;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MimicKeybindAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_DELIMITER = ";";
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

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            LOGGER.warn("[FANCYMENU] Unable to mimic keybind because options are not initialized.");
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
        boolean keepPressed = config.keepPressed && (config.pressedDurationMs > 0L);
        long holdDuration = keepPressed ? Math.max(config.pressedDurationMs, 1L) : 0L;

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
        if (handler == null) {
            return false;
        }

        long window = minecraft.getWindow().getWindow();
        int keyCode;
        int scanCode;
        if (key.getType() == InputConstants.Type.KEYSYM) {
            keyCode = key.getValue();
            scanCode = GLFW.glfwGetKeyScancode(keyCode);
            if (scanCode == GLFW.GLFW_KEY_UNKNOWN) {
                scanCode = -1;
            }
        } else {
            keyCode = InputConstants.UNKNOWN.getValue();
            scanCode = key.getValue();
        }

        KeyMapping.set(key, true);
        handler.keyPress(window, keyCode, scanCode, 1, 0);

        if (keepPressed) {
            startHoldThread(keyMapping, key, keyCode, scanCode, true, holdDurationMs);
        } else {
            int finalScanCode = scanCode;
            MainThreadTaskExecutor.executeInMainThread(() -> releaseKeyboardKey(key, keyCode, finalScanCode), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }
        return true;
    }

    private void releaseKeyboardKey(@NotNull InputConstants.Key key, int keyCode, int scanCode) {
        Minecraft minecraft = Minecraft.getInstance();
        KeyboardHandler handler = minecraft.keyboardHandler;
        if (handler != null) {
            handler.keyPress(minecraft.getWindow().getWindow(), keyCode, scanCode, 0, 0);
        }
        KeyMapping.set(key, false);
    }

    private void startHoldThread(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key, int keyCode, int scanCode, boolean keyboard, long durationMs) {
        Thread holdThread = new Thread(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            long window = minecraft.getWindow().getWindow();
            long start = System.currentTimeMillis();
            while (minecraft.isRunning() && !Thread.currentThread().isInterrupted() && (System.currentTimeMillis() - start) < durationMs) {
                if (!keyMapping.isDown()) {
                    minecraft.execute(() -> {
                        if (keyboard) {
                            KeyMapping.set(key, true);
                            KeyboardHandler handler = minecraft.keyboardHandler;
                            if (handler != null) {
                                handler.keyPress(window, keyCode, scanCode, 1, 0);
                            }
                        } else {
                            KeyMapping.set(key, true);
                            KeyMapping.click(key);
                        }
                    });
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
        holdThread.setDaemon(true);
        holdThread.start();
    }

    @Nullable
    protected KeyMapping findKeyMapping(@NotNull String identifier) {
        String trimmedId = identifier.trim();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            return null;
        }
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
            Screen current = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(NotificationScreen.error(callback -> Minecraft.getInstance().setScreen(current), LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimic_keybind.error")));
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.mimic_keybind");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimic_keybind.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return "key.jump;false;1000";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        MimicKeybindActionValueScreen screen = new MimicKeybindActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValueExample()), value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(screen);
    }

    @NotNull
    protected static List<String> getAvailableKeybindNames() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            return new ArrayList<>(Collections.singletonList("[NO KEYBINDS FOUND]"));
        }
        List<String> names = new ArrayList<>();
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            names.add(keyMapping.getName());
        }
        if (names.isEmpty()) {
            names.add("[NO KEYBINDS FOUND]");
        }
        return names;
    }

    public static class MimicKeybindActionValueScreen extends StringBuilderScreen {

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

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.keep_pressed"));
            CycleButton<CommonCycles.CycleEnabledDisabled> keepPressedButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.mimic_keybind.edit.keep_pressed", this.config.keepPressed),
                    (value, button) -> this.config.keepPressed = value.getAsBoolean());
            keepPressedButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimic_keybind.edit.keep_pressed.desc")));
            this.addWidgetCell(keepPressedButton, true);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.pressed_duration"));
            TextInputCell durationInput = this.addTextInputCell(CharacterFilter.buildIntegerFiler(), true, true)
                    .setEditListener(s -> this.config.pressedDurationMs = parseDuration(s))
                    .setText(Long.toString(this.config.pressedDurationMs));
            durationInput.editBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("fancymenu.actions.mimic_keybind.edit.pressed_duration.desc")));

            this.addCellGroupEndSpacerCell();

            this.keyInfoLabel = this.addLabelCell(this.buildCurrentKeyInfoComponent());

            this.addSpacerCell(20);
        }

        private long parseDuration(String value) {
            if (value == null || value.trim().isEmpty()) {
                return DEFAULT_KEEP_DURATION_MS;
            }
            try {
                long parsed = Long.parseLong(value.trim());
                if (parsed < 0L) {
                    parsed = 0L;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                return DEFAULT_KEEP_DURATION_MS;
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
            if (minecraft.options == null) {
                return null;
            }
            for (KeyMapping keyMapping : minecraft.options.keyMappings) {
                if (keyMapping.getName().equals(identifier.trim())) {
                    return keyMapping;
                }
            }
            return null;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
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
            return !this.config.keybindName.trim().isEmpty() && (!this.config.keepPressed || this.config.pressedDurationMs > 0L);
        }

        @Override
        public @NotNull String buildString() {
            return this.config.serialize();
        }

    }

    protected static class MimicKeybindConfig {

        protected String keybindName = "";
        protected boolean keepPressed = false;
        protected long pressedDurationMs = DEFAULT_KEEP_DURATION_MS;

        protected static MimicKeybindConfig parse(@Nullable String rawValue) {
            MimicKeybindConfig config = new MimicKeybindConfig();
            if (rawValue == null || rawValue.isEmpty()) {
                return config;
            }

            String[] parts = rawValue.split(VALUE_DELIMITER, -1);
            if (parts.length >= 1) {
                config.keybindName = parts[0];
            }
            if (parts.length >= 2) {
                config.keepPressed = Boolean.parseBoolean(parts[1]);
            }
            if (parts.length >= 3) {
                try {
                    config.pressedDurationMs = Math.max(0L, Long.parseLong(parts[2].trim()));
                } catch (NumberFormatException ignored) {
                    config.pressedDurationMs = DEFAULT_KEEP_DURATION_MS;
                }
            }
            if (parts.length == 1) {
                config.keepPressed = false;
                config.pressedDurationMs = DEFAULT_KEEP_DURATION_MS;
            }
            return config;
        }

        protected String serialize() {
            String sanitizedName = this.keybindName.trim();
            if (sanitizedName.isEmpty()) {
                sanitizedName = "";
            }
            long duration = Math.max(0L, this.pressedDurationMs);
            return sanitizedName + VALUE_DELIMITER + this.keepPressed + VALUE_DELIMITER + duration;
        }
    }
}
