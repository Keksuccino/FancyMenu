package de.keksuccino.fancymenu.api.placeholder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Deprecated
public class PlaceholderTextRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, PlaceholderTextContainer> PLACEHOLDERS = new LinkedHashMap<>();

    @Deprecated
    public static void registerPlaceholder(PlaceholderTextContainer placeholder) {
        if (placeholder != null) {
            if (placeholder.getIdentifier() != null) {
                if (PLACEHOLDERS.containsKey(placeholder.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] Placeholder text with identifier '" + placeholder.getIdentifier() + "' is already registered! Overriding item!");
                }
                PLACEHOLDERS.put(placeholder.getIdentifier(), placeholder);
            } else {
                LOGGER.error("[FANCYMENU] Placeholder identifier cannot be NULL for PlaceholderTextContainers!");
            }
        }
    }

    /**
     * Unregister a previously added placeholder.
     */
    @Deprecated
    public static void unregisterPlaceholder(String placeholderIdentifier) {
        PLACEHOLDERS.remove(placeholderIdentifier);
    }

    /**
     * Get all registered placeholders as list.
     */
    @Deprecated
    public static List<PlaceholderTextContainer> getPlaceholders() {
        List<PlaceholderTextContainer> l = new ArrayList<>();
        l.addAll(PLACEHOLDERS.values());
        return l;
    }

    /**
     * Get a registered placeholder by its identifier.
     */
    @Deprecated
    public static PlaceholderTextContainer getPlaceholder(String placeholderIdentifier) {
        return PLACEHOLDERS.get(placeholderIdentifier);
    }

}
