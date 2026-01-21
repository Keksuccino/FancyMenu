package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsVariableValueRequirement extends Requirement {

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
        return I18n.get("fancymenu.requirements.is_variable_value");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_variable_value.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return "";
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
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        IsVariableValueConfigScreen s = new IsVariableValueConfigScreen(Objects.requireNonNullElse(instance.value, ""), callback -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            if (callback != null) {
                String oldValue = instance.value;
                instance.value = callback;
                onEditingCompleted.accept(instance, oldValue, callback);
            } else {
                onEditingCanceled.accept(instance);
            }
            closeAction[0].run();
        });
        closeAction[0] = Requirement.openRequirementValueEditor(s, () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    public static class IsVariableValueConfigScreen extends Requirement.RequirementValueEditScreen {

        @NotNull
        protected String variableName = "";
        @NotNull
        protected String variableValue = "";

        protected TextInputCell nameCell;
        protected TextInputCell valueCell;
        protected EditBoxSuggestions variableNameSuggestions;

        protected IsVariableValueConfigScreen(String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.requirements.is_variable_value.value.desc"), callback);
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
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_variable_value.var_name"));
            this.nameCell = this.addTextInputCell(null, true, true).setText(name);

            this.addCellGroupEndSpacerCell();

            this.variableNameSuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.nameCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, VariableHandler.getVariableNames());
            UIBase.applyDefaultWidgetSkinTo(this.variableNameSuggestions);
            this.nameCell.editBox.setResponder(s -> this.variableNameSuggestions.updateCommandInfo());

            this.addCellGroupEndSpacerCell();

            String value = this.getVarValueString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_variable_value.var_value"));
            this.valueCell = this.addTextInputCell(null, true, true).setText(value);

            this.addSpacerCell(20);

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.variableNameSuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.variableNameSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.variableNameSuggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
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
