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

    public static final SetVariableAction SET_VARIABLE = new SetVariableAction();
    public static final ClearVariablesAction CLEAR_VARIABLES = new ClearVariablesAction();
    public static final PasteToChatAction PASTE_TO_CHAT = new PasteToChatAction();
    public static final ToggleLayoutAction TOGGLE_LAYOUT = new ToggleLayoutAction();
    public static final EnableLayoutAction ENABLE_LAYOUT = new EnableLayoutAction();
    public static final DisableLayoutAction DISABLE_LAYOUT = new DisableLayoutAction();

    public static void registerAll() {

        ActionRegistry.registerAction(SET_VARIABLE);
        ActionRegistry.registerAction(CLEAR_VARIABLES);
        ActionRegistry.registerAction(PASTE_TO_CHAT);
        ActionRegistry.registerAction(TOGGLE_LAYOUT);
        ActionRegistry.registerAction(ENABLE_LAYOUT);
        ActionRegistry.registerAction(DISABLE_LAYOUT);

        for (Action b : LegacyActions.buildLegacyActionContainers()) {
            ActionRegistry.registerAction(b);
        }

    }

}
