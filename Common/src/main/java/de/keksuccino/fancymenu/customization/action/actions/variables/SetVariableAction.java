package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import net.minecraft.client.resources.language.I18n;

public class SetVariableAction extends Action {

    public SetVariableAction() {
        super("set_variable");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {
        if (value != null) {
            if (value.contains(":")) {
                String name = value.split(":", 2)[0];
                String val = value.split(":", 2)[1];
                VariableHandler.setVariable(name, val);
            }
        }
    }

    @Override
    public String getActionDescription() {
        return I18n.get("fancymenu.helper.buttonaction.variables.set.desc");
    }

    @Override
    public String getValueDescription() {
        return I18n.get("fancymenu.helper.buttonaction.variables.set.value.desc");
    }

    @Override
    public String getValueExample() {
        return "cool_variable_name:some_value";
    }

}
