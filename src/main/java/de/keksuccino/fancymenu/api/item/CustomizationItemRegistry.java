package de.keksuccino.fancymenu.api.item;

import de.keksuccino.fancymenu.FancyMenu;

import java.util.*;

public class CustomizationItemRegistry {

    protected static Map<String, CustomizationItemContainer> customizationItems = new LinkedHashMap<>();

    /**
     * Register your own customization items here.
     */
    public static void registerItem(CustomizationItemContainer item) {
        if (item != null) {
            if (item.getIdentifier() != null) {
                if (customizationItems.containsKey(item.getIdentifier())) {
                    FancyMenu.LOGGER.warn("[FANCYMENU] WARNING! A customization item with the identifier '" + item.getIdentifier() + "' is already registered! Overriding item!");
                }
                customizationItems.put(item.getIdentifier(), item);
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ERROR! Item identifier cannot be null for CustomizationItemContainers!");
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
