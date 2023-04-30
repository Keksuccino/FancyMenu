package de.keksuccino.fancymenu.customization.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

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
                String name = value.split("[:]", 2)[0];
                String val = value.split("[:]", 2)[1];
                String storedVal = VariableHandler.getVariable(name);
                if (storedVal != null) {
                    return val.equals(storedVal);
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_variable_value");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.is_variable_value.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_variable_value.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "<variable_name>:<value_to_check_for>";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
