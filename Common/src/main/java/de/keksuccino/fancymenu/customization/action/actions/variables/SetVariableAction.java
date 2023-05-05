package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.konkrete.localization.Locals;

public class SetVariableAction extends ButtonActionContainer {

    public SetVariableAction() {
        super("fancymenu_buttonaction_setvariable");
    }

    @Override
    public String getAction() {
        return "set_variable";
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {
        if (value != null) {
            if (value.contains(":")) {
                String name = value.split("[:]", 2)[0];
                String val = value.split("[:]", 2)[1];
                VariableHandler.setVariable(name, val);
            }
        }
    }

    @Override
    public String getActionDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.variables.set.desc");
    }

    @Override
    public String getValueDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.variables.set.value.desc");
    }

    @Override
    public String getValueExample() {
        return "cool_variable_name:some_value";
    }

}
