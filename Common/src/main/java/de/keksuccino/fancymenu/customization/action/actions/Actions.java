package de.keksuccino.fancymenu.customization.action.actions;

import de.keksuccino.fancymenu.customization.action.LegacyActions;
import de.keksuccino.fancymenu.customization.action.actions.layout.DisableLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.layout.EnableLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.layout.ToggleLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.other.PasteToChatAction;
import de.keksuccino.fancymenu.customization.action.actions.variables.ClearVariablesAction;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;

public class Actions {

    public static void registerAll() {

        ActionRegistry.registerAction(new SetVariableAction());
        ActionRegistry.registerAction(new ClearVariablesAction());

        ActionRegistry.registerAction(new PasteToChatAction());

        ActionRegistry.registerAction(new ToggleLayoutAction());
        ActionRegistry.registerAction(new EnableLayoutAction());
        ActionRegistry.registerAction(new DisableLayoutAction());

        for (Action b : LegacyActions.buildLegacyActionContainers()) {
            ActionRegistry.registerAction(b);
        }

    }

}
