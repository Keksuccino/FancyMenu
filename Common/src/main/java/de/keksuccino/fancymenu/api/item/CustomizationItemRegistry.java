package de.keksuccino.fancymenu.api.item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CustomizationItemRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static Map<String, CustomizationItemContainer> customizationItems = new LinkedHashMap<>();

    /**
     * Register your own customization items here.
     */
    public static void registerItem(CustomizationItemContainer item) {
        if (item != null) {
            if (item.getIdentifier() != null) {
                if (customizationItems.containsKey(item.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] Customization item with identifier '" + item.getIdentifier() + "' is already registered! Overriding item!");
                }
                customizationItems.put(item.getIdentifier(), item);
            } else {
                LOGGER.error("[FANCYMENU] Item identifier cannot be NULL for CustomizationItemContainers!");
            }
        }
    }

    /**
     * Unregister a previously added item.
     */
    public static void unregisterItem(String itemIdentifier) {
        customizationItems.remove(itemIdentifier);
    }

    /**
     * Get all registered items as list.
     */
    public static List<CustomizationItemContainer> getItems() {
        List<CustomizationItemContainer> l = new ArrayList<>();
        l.addAll(customizationItems.values());
        return l;
    }

    /**
     * Get a registered item by its identifier.
     */
    public static CustomizationItemContainer getItem(String itemIdentifier) {
        return customizationItems.get(itemIdentifier);
    }

}
