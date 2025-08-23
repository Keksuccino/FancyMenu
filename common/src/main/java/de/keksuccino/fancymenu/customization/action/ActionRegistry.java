package de.keksuccino.fancymenu.customization.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ActionRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Action> ACTIONS = new LinkedHashMap<>();

    public static void register(@NotNull Action action) {
        if (action.getIdentifier().contains(":")) {
            LOGGER.error("[FANCYMENU] Tried to register action with illegal character ':' in identifier: " + action.getIdentifier());
            throw new RuntimeException("Illegal character ':' used in FancyMenu action identifier: " + action.getIdentifier());
        }
        if (ACTIONS.containsKey(Objects.requireNonNull(action.getIdentifier()))) {
            LOGGER.warn("[FANCYMENU] An action with the identifier '" + action.getIdentifier() + "' is already registered! Overriding action!");
        }
        ACTIONS.put(action.getIdentifier(), action);
    }

    @NotNull
    public static List<Action> getActions() {
        List<Action> l = new ArrayList<>();
        ACTIONS.forEach((key, value) -> l.add(value));
        return l;
    }

    @Nullable
    public static Action getAction(@NotNull String identifier) {
        return ACTIONS.get(identifier);
    }

}
