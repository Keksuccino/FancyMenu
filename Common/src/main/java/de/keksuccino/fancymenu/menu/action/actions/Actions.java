package de.keksuccino.fancymenu.menu.action.actions;

import de.keksuccino.fancymenu.menu.action.LegacyActions;
import de.keksuccino.fancymenu.menu.action.actions.layout.EnableLayoutButtonAction;
import de.keksuccino.fancymenu.menu.action.actions.layout.ToggleLayoutButtonAction;
import de.keksuccino.fancymenu.menu.action.actions.other.PasteToChatAction;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.menu.action.actions.layout.DisableLayoutButtonAction;
import de.keksuccino.fancymenu.menu.action.actions.variables.ClearVariablesAction;
import de.keksuccino.fancymenu.menu.action.actions.variables.SetVariableAction;

public class Actions {

    public static void registerAll() {

        ButtonActionRegistry.registerButtonAction(new SetVariableAction());
        ButtonActionRegistry.registerButtonAction(new ClearVariablesAction());

        ButtonActionRegistry.registerButtonAction(new PasteToChatAction());

        ButtonActionRegistry.registerButtonAction(new ToggleLayoutButtonAction());
        ButtonActionRegistry.registerButtonAction(new EnableLayoutButtonAction());
        ButtonActionRegistry.registerButtonAction(new DisableLayoutButtonAction());

        for (ButtonActionContainer b : LegacyActions.buildLegacyActionContainers()) {
            ButtonActionRegistry.registerButtonAction(b);
        }

    }

}
