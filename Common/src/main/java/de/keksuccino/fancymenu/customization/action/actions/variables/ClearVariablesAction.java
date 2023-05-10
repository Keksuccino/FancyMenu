package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.konkrete.localization.Locals;

public class ClearVariablesAction extends Action {

    public ClearVariablesAction() {
        super("clear_variables");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(String value) {
        VariableHandler.clearVariables();
    }

    @Override
    public String getActionDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.variables.clearall.desc");
    }

    @Override
    public String getValueDescription() {
        return null;
    }

    @Override
    public String getValueExample() {
        return null;
    }

}
