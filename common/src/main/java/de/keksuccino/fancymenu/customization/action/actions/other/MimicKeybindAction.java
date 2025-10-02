package de.keksuccino.fancymenu.customization.action.actions.other;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MimicKeybindAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
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
        if (value == null || value.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            LOGGER.warn("[FANCYMENU] Unable to mimic keybind because options are not initialized.");
            return;
        }
        KeyMapping keyMapping = findKeyMapping(value);
        if (keyMapping == null) {
            handleMissingKeybind(value);
            return;
        }
        InputConstants.Key key = Services.PLATFORM.getKeyMappingKey(keyMapping);
        if (key == null) {
            LOGGER.error("[FANCYMENU] MimicKeybindAction could not resolve bound key for '{}'!", value);
            handleMissingKeybind(value);
            return;
        }
        if (!this.triggerKeybind(keyMapping, key)) {
            LOGGER.error("[FANCYMENU] MimicKeybindAction does not support key type '{}' for '{}'!", key.getType(), value);
            handleMissingKeybind(value);
        }
    }


    protected boolean triggerKeybind(@NotNull KeyMapping keyMapping, @NotNull InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.MOUSE) {
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            MainThreadTaskExecutor.executeInMainThread(() -> KeyMapping.set(key, false), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            return true;
        }
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
        } else if (key.getType() == InputConstants.Type.SCANCODE) {
            keyCode = InputConstants.UNKNOWN.getValue();
            scanCode = key.getValue();
        } else {
            return false;
        }
        final int pressKeyCode = keyCode;
        final int pressScanCode = scanCode;
        handler.keyPress(window, pressKeyCode, pressScanCode, 1, 0);
        MainThreadTaskExecutor.executeInMainThread(() -> handler.keyPress(window, pressKeyCode, pressScanCode, 0, 0), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return true;
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
        return "key.jump";
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

        @NotNull
        protected String keybindName = "";
        protected EditBoxSuggestions suggestions;
        @Nullable
        protected LabelCell keyInfoLabel;

        protected MimicKeybindActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.mimic_keybind.edit.title"), callback);
            this.keybindName = value;
        }

        @Override
        protected void initCells() {
            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.mimic_keybind.edit.keybind"));
            TextInputCell keybindInput = this.addTextInputCell(null, true, true).setText(this.keybindName);

            this.suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, keybindInput.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, getAvailableKeybindNames());
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            keybindInput.editBox.setResponder(text -> {
                this.keybindName = text;
                this.suggestions.updateCommandInfo();
                if (this.keyInfoLabel != null) {
                    this.keyInfoLabel.setText(this.buildCurrentKeyInfoComponent());
                }
            });

            this.addCellGroupEndSpacerCell();

            this.keyInfoLabel = this.addLabelCell(this.buildCurrentKeyInfoComponent());

            this.addSpacerCell(20);
        }

        @NotNull
        protected Component buildCurrentKeyInfoComponent() {
            KeyMapping keyMapping = getKeyMapping(this.keybindName);
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
            return !this.keybindName.trim().isEmpty();
        }

        @Override
        public @NotNull String buildString() {
            return this.keybindName.trim();
        }

    }

}
