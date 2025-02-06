package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.gui;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsKeyPressedRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsKeyPressedRequirement() {
        super("is_key_pressed");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            int keycode = SerializationUtils.deserializeNumber(Integer.class, -1, value);
            com.mojang.blaze3d.platform.InputConstants.Key key = getKey(keycode);
            return (key.getValue() != -1) && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
        }

        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_key_pressed");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_key_pressed.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.gui");
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "-1:-1";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsKeyPressedValueConfigScreen s = new IsKeyPressedValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, ""), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    @NotNull
    public static com.mojang.blaze3d.platform.InputConstants.Key getKey(int keyCode) {
        com.mojang.blaze3d.platform.InputConstants.Key key = null;
        try {
            key = InputConstants.getKey(keyCode, -1);
        } catch (Exception ignore) {}
        return (key != null) ? key : InputConstants.UNKNOWN;
    }

    public static class IsKeyPressedValueConfigScreen extends StringBuilderScreen {

        protected int keyCode;
        protected boolean keyInputModeEnabled = false;

        protected LabelCell selectedKeyLabel = null;
        protected LabelCell pressNowLabel = null;

        protected IsKeyPressedValueConfigScreen(String value, @NotNull Consumer<String> callback) {
            super(Components.translatable("fancymenu.requirements.is_key_pressed.screen.title"), callback);
            if (value == null) value = "";
            this.keyCode = SerializationUtils.deserializeNumber(Integer.class, -1, value);
            if (this.keyCode == -1) this.keyCode = InputConstants.KEY_G;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            InputConstants.Key key = getKey(this.keyCode);

            this.selectedKeyLabel = this.addLabelCell(Components.translatable("fancymenu.requirements.is_key_pressed.screen.selected_key", ((MutableComponent)key.getDisplayName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()))));

            this.addWidgetCell(new ExtendedButton(0, 0, 0, 20, Components.translatable("fancymenu.requirements.is_key_pressed.screen.change_key"), button -> {
                this.keyInputModeEnabled = true;
                this.pressNowLabel.setText(Components.translatable("fancymenu.requirements.is_key_pressed.screen.change_key.press_now").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
                button.setFocused(false);
            }).setIsActiveSupplier(consumes -> !this.keyInputModeEnabled), true);

            this.pressNowLabel = this.addLabelCell(Components.empty());

            this.addSpacerCell(20);

        }

        @Override
        public boolean keyPressed(int keycode, int scancode, int modifiers) {
            if (this.keyInputModeEnabled) {
                this.keyCode = keycode;
                this.keyInputModeEnabled = false;
                this.pressNowLabel.setText(Components.empty());
                InputConstants.Key key = getKey(this.keyCode);
                this.selectedKeyLabel.setText(Components.translatable("fancymenu.requirements.is_key_pressed.screen.selected_key", ((MutableComponent)key.getDisplayName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()))));
                return true;
            }
            return super.keyPressed(keycode, scancode, modifiers);
        }

        @Override
        public @NotNull String buildString() {
            return "" + this.keyCode;
        }

    }

}
