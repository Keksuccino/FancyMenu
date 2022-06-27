//TODO übernehmen
package de.keksuccino.fancymenu.menu.button.buttonactions;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.menu.button.buttonactions.layout.DisableLayoutButtonAction;
import de.keksuccino.fancymenu.menu.button.buttonactions.layout.EnableLayoutButtonAction;
import de.keksuccino.fancymenu.menu.button.buttonactions.layout.ToggleLayoutButtonAction;
import de.keksuccino.fancymenu.menu.button.buttonactions.variables.ClearVariablesButtonAction;
import de.keksuccino.fancymenu.menu.button.buttonactions.variables.SetVariableButtonAction;

public class ButtonActions {

    public static void registerAll() {

        ButtonActionRegistry.registerButtonAction(new SetVariableButtonAction());
        ButtonActionRegistry.registerButtonAction(new ClearVariablesButtonAction());

        ButtonActionRegistry.registerButtonAction(new PasteToChatButtonAction());

        ButtonActionRegistry.registerButtonAction(new ToggleLayoutButtonAction());
        ButtonActionRegistry.registerButtonAction(new EnableLayoutButtonAction());
        ButtonActionRegistry.registerButtonAction(new DisableLayoutButtonAction());

    }

}
