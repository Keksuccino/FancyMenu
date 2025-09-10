package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ListenerRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CharacterFilter IDENTIFIER_NAME_VALIDATOR = CharacterFilter.buildResourceNameFilter();
    private static final Map<String, AbstractListener> LISTENERS = new LinkedHashMap<>();

    public static void register(@NotNull AbstractListener listener) {
        if (!ListenerHandler.canRegisterListeners) {
            throw new RuntimeException("[FANCYMENU] Tried to register listener too late: " + listener.getIdentifier());
        }
        if (!IDENTIFIER_NAME_VALIDATOR.isAllowedText(listener.getIdentifier())) {
            throw new RuntimeException("[FANCYMENU] Failed to register listener! Listener identifiers can only have basic characters [a-z], [0-9], [-_]! Illegal identifier: " + listener.getIdentifier());
        }
        if (getListener(listener.getIdentifier()) != null) {
            LOGGER.error("[FANCYMENU] Failed to register listener! Another listener with the same identifier is already registered: " + listener.getIdentifier(), new IllegalStateException("Duplicate listener identifier"));
            return;
        }
        LISTENERS.put(listener.getIdentifier(), listener);
    }

    @Nullable
    public static AbstractListener getListener(@NotNull String identifier) {
        return LISTENERS.get(identifier);
    }

    @NotNull
    public static List<AbstractListener> getListeners() {
        return new ArrayList<>(LISTENERS.values());
    }

}
