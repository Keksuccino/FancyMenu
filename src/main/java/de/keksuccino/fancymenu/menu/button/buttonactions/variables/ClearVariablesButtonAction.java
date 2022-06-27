//TODO übernehmen
package de.keksuccino.fancymenu.menu.button.buttonactions.variables;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.localization.Locals;

public class ClearVariablesButtonAction extends ButtonActionContainer {

    public ClearVariablesButtonAction() {
        super("fancymenu_buttonaction_clear_all_variables");
    }

    @Override
    public String getAction() {
        return "clear_variables";
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
