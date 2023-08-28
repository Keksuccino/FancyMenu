package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsVariableValueRequirement extends LoadingRequirement {

    public IsVariableValueRequirement() {
        super("fancymenu_visibility_requirement_is_variable_value");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            if (value.contains(":")) {
                String name = value.split(":", 2)[0];
                String val = value.split(":", 2)[1];
                if (VariableHandler.variableExists(name)) {
                    String storedVal = Objects.requireNonNull(VariableHandler.getVariable(name)).getValue();
                    return val.equals(storedVal);
                }
            }
        }

        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.visibilityrequirement.is_variable_value");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.visibilityrequirement.is_variable_value.desc")));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.visibilityrequirement.is_variable_value.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "<variable_name>:<value_to_check_for>";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsVariableValueConfigScreen s = new IsVariableValueConfigScreen(requirementInstance.value, callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class IsVariableValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected String variableName = "";
        @NotNull
        protected String variableValue = "";

        protected TextInputCell nameCell;
        protected TextInputCell valueCell;
        protected EditBoxSuggestions variableNameSuggestions;

        protected IsVariableValueConfigScreen(String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.editor.elements.visibilityrequirements.edit_value"), callback);
            if (value == null) value = "";
            if (value.contains(":")) {
                this.variableName = value.split(":", 2)[0];
                this.variableValue = value.split(":", 2)[1];
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String name = this.getVarNameString();
            this.addLabelCell(Component.translatable("fancymenu.loading_requirements.is_variable_value.var_name"));
            this.nameCell = this.addTextInputCell(null, true, true).setText(name);

            this.variableNameSuggestions = new EditBoxSuggestions(Minecraft.getInstance(), this, this.nameCell.editBox, Minecraft.getInstance().font, false, true, 0, 7, false, Integer.MIN_VALUE);
            this.variableNameSuggestions.setAllowSuggestions(true);
            //TODO FIXEN: custom suggestions gehen nicht
//            this.variableNameSuggestions.enableOnlyCustomSuggestionsMode(true);
//            this.variableNameSuggestions.setCustomSuggestions(VariableHandler.getVariableNames());
            this.variableNameSuggestions.updateCommandInfo();
            this.nameCell.editBox.setResponder(s -> this.variableNameSuggestions.updateCommandInfo());

            String value = this.getVarValueString();
            this.addLabelCell(Component.translatable("fancymenu.loading_requirements.is_variable_value.var_value"));
            this.valueCell = this.addTextInputCell(null, true, true).setText(value);

        }

        @Override
        public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
            super.render(pose, mouseX, mouseY, partial);
            this.variableNameSuggestions.render(pose, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.variableNameSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double $$2) {
            if (this.variableNameSuggestions.mouseScrolled($$2)) return true;
            return super.mouseScrolled($$0, $$1, $$2);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.variableNameSuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.getVarNameString() + ":" + this.getVarValueString();
        }

        @NotNull
        protected String getVarNameString() {
            if (this.nameCell != null) {
                return this.nameCell.getText();
            }
            return this.variableName;
        }

        @NotNull
        protected String getVarValueString() {
            if (this.valueCell != null) {
                return this.valueCell.getText();
            }
            return this.variableValue;
        }

    }

}
