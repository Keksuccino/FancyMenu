package de.keksuccino.fancymenu.api.buttonaction;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ButtonActionRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static Map<String, ButtonActionContainer> actions = new LinkedHashMap<>();

    /**
     * Register your custom button actions here.
     */
    public static void registerButtonAction(ButtonActionContainer action) {
        if (action != null) {
            if (action.getIdentifier() != null) {
                if (actions.containsKey(action.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] An action with the identifier '" + action.getIdentifier() + "' is already registered! Overriding action!");
                }
                ButtonActionContainer c = getActionByName(action.getAction());
                if (c != null) {
                    LOGGER.warn("[FANCYMENU] An action with the name '" + action.getAction() + "' is already registered! Overriding action!");
                    unregisterButtonAction(c.getIdentifier());
                }
                actions.put(action.getIdentifier(), action);
            } else {
                LOGGER.error("[FANCYMENU] Action identifier cannot be null for ButtonActionContainers!");
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
        actions.forEach((key, value) -> {
            l.add(value);
        });
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
