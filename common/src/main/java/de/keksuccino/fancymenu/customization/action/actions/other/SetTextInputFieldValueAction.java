package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.inputfield.InputFieldElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class SetTextInputFieldValueAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";

    public SetTextInputFieldValueAction() {
        super("set_text_input_field_value");
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
        try {
            Config config = Config.parse(value);
            if (config == null || config.elementIdentifier.isBlank()) {
                return;
            }

            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) {
                return;
            }

            AbstractElement element = findElement(config.elementIdentifier);
            if (element == null) {
                return;
            }

            if (element instanceof InputFieldElement inputFieldElement) {
                if (inputFieldElement.editBox == null) {
                    return;
                }
                if (!config.forceSetWhenInactive && !inputFieldElement.editBox.active) {
                    return;
                }
                inputFieldElement.editBox.setValue(config.newValue);
                return;
            }

            if (element instanceof VanillaWidgetElement widgetElement) {
                if (widgetElement.getWidget() instanceof EditBox editBox) {
                    if (!config.forceSetWhenInactive && !editBox.active) {
                        return;
                    }
                    editBox.setValue(config.newValue);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute SetTextInputFieldValueAction!", ex);
        }
    }

    @Nullable
    private static AbstractElement findElement(@NotNull String id) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return null;
        }

        if (screen instanceof LayoutEditorScreen editor) {
            AbstractEditorElement e = editor.getElementByInstanceIdentifier(id);
            return e != null ? e.element : null;
        }

        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
        if (layer == null) {
            return null;
        }
        return layer.getElementByInstanceIdentifier(id);
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.set_text_input_field_value");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.set_text_input_field_value.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.empty(); // handled by custom value edit screen
    }

    @Override
    public @Nullable String getValuePreset() {
        return "some.element.id" + VALUE_SEPARATOR + "Hello World!" + VALUE_SEPARATOR + "false";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};
        final PiPWindow[] windowHolder = new PiPWindow[1];
        SetTextInputFieldValueActionValueScreen s = new SetTextInputFieldValueActionValueScreen(
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
                }
        );
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

    public static final class Config {
        public String elementIdentifier = "";
        public String newValue = "";
        public boolean forceSetWhenInactive = false;

        @NotNull
        public String serialize() {
            return this.elementIdentifier + VALUE_SEPARATOR + this.newValue + VALUE_SEPARATOR + this.forceSetWhenInactive;
        }

        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) {
                return null;
            }

            String[] parts = value.split("\\|\\|\\|", -1);
            Config config = new Config();
            if (parts.length >= 1) config.elementIdentifier = parts[0];
            if (parts.length >= 2) config.newValue = parts[1];
            if (parts.length >= 3) config.forceSetWhenInactive = Boolean.parseBoolean(parts[2]);
            return config;
        }
    }

    public static class SetTextInputFieldValueActionValueScreen extends CellScreen {

        protected Config config;
        protected Consumer<String> callback;

        protected SetTextInputFieldValueActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.set_text_input_field_value.edit_value"));
            this.callback = callback;
            this.config = Objects.requireNonNullElse(Config.parse(value), new Config());
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.set_text_input_field_value.edit.element_identifier"));
            TextInputCell identifierCell = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.elementIdentifier = s)
                    .setText(this.config.elementIdentifier)
                    .setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.actions.set_text_input_field_value.edit.element_identifier.desc")));

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.set_text_input_field_value.edit.value"));
            TextInputCell valueCell = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.newValue = s)
                    .setText(this.config.newValue)
                    .setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.actions.set_text_input_field_value.edit.value.desc")));

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> forceSetButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.actions.set_text_input_field_value.edit.force_set_when_inactive", this.config.forceSetWhenInactive),
                    (val, button) -> this.config.forceSetWhenInactive = val.getAsBoolean()
            );
            forceSetButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.actions.set_text_input_field_value.edit.force_set_when_inactive.desc")));
            this.addWidgetCell(forceSetButton, true);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.config.elementIdentifier.isBlank();
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
