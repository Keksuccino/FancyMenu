//TODO übernehmen
package de.keksuccino.fancymenu.api.buttonaction;

import de.keksuccino.fancymenu.FancyMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonActionRegistry {

    protected static Map<String, ButtonActionContainer> actions = new HashMap<>();

    /**
     * Register your custom button actions here.
     */
    public static void registerButtonAction(ButtonActionContainer action) {
        if (action != null) {
            if (action.getIdentifier() != null) {
                if (actions.containsKey(action.getIdentifier())) {
                    FancyMenu.LOGGER.warn("[FANCYMENU] WARNING! A button action with the identifier '" + action.getIdentifier() + "' is already registered! Overriding action!");
                }
                actions.put(action.getIdentifier(), action);
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ERROR! Action identifier cannot be null for ButtonActionContainers!");
            }
        }
    }

    /**
     * Unregister a previously added button action.
     */
    public static void unregisterButtonAction(String actionIdentifier) {
        actions.remove(actionIdentifier);
    }

    public static List<ButtonActionContainer> getActions() {
        List<ButtonActionContainer> l = new ArrayList<>();
        l.addAll(actions.values());
        return l;
    }

    public static ButtonActionContainer getAction(String actionIdentifier) {
        return actions.get(actionIdentifier);
    }

    public static ButtonActionContainer getActionByName(String actionName) {
        for (ButtonActionContainer c : actions.values()) {
            if ((c.getAction() != null) && c.getAction().equals(actionName)) {
                return c;
            }
        }
        return null;
    }

}
