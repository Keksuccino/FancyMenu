package de.keksuccino.fancymenu.api.placeholder;

import de.keksuccino.fancymenu.FancyMenu;

import java.util.*;

@Deprecated
public class PlaceholderTextRegistry {

    private static Map<String, PlaceholderTextContainer> placeholders = new LinkedHashMap<>();

    @Deprecated
    public static void registerPlaceholder(PlaceholderTextContainer placeholder) {
        if (placeholder != null) {
            if (placeholder.getIdentifier() != null) {
                if (placeholders.containsKey(placeholder.getIdentifier())) {
                    FancyMenu.LOGGER.warn("[FANCYMENU] WARNING! A placeholder text with the identifier '" + placeholder.getIdentifier() + "' is already registered! Overriding item!");
                }
                placeholders.put(placeholder.getIdentifier(), placeholder);
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ERROR! Placeholder identifier cannot be null for PlaceholderTextContainers!");
            }
        }
    }

    /**
     * Unregister a previously added placeholder.
     */
    @Deprecated
    public static void unregisterPlaceholder(String placeholderIdentifier) {
        placeholders.remove(placeholderIdentifier);
    }

    /**
     * Get all registered placeholders as list.
     */
    @Deprecated
    public static List<PlaceholderTextContainer> getPlaceholders() {
        List<PlaceholderTextContainer> l = new ArrayList<>();
        l.addAll(placeholders.values());
        return l;
    }

    /**
     * Get a registered placeholder by its identifier.
     */
    @Deprecated
    public static PlaceholderTextContainer getPlaceholder(String placeholderIdentifier) {
        return placeholders.get(placeholderIdentifier);
    }

}
