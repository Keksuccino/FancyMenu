package de.keksuccino.fancymenu.api.background;

import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class MenuBackgroundTypeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static Map<String, MenuBackgroundType> backgroundTypes = new LinkedHashMap<>();

    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            EventHandler.INSTANCE.registerListenersOf(new MenuBackgroundTypeRegistry());
            initialized = true;
        }
    }

    /**
     * Register your custom menu background type here.
     */
    public static void registerBackgroundType(MenuBackgroundType type) {
        if (type != null) {
            if (type.getIdentifier() != null) {
                if (backgroundTypes.containsKey(type.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] Menu background with the identifier '" + type.getIdentifier() + "' is already registered! Overriding background!");
                }
                backgroundTypes.put(type.getIdentifier(), type);
                type.loadBackgrounds();
            } else {
                LOGGER.error("[FANCYMENU] Failed to register menu background! Identifier cannot be NULL!");
            }
        } else {
            LOGGER.error("[FANCYMENU] Failed to register menu background! Menu background type cannot be NULL!");
        }
    }

    /**
     * Unregister a menu background type.
     */
    public static void unregisterBackgroundType(String typeIdentifier) {
        backgroundTypes.remove(typeIdentifier);
    }

    public static List<MenuBackgroundType> getBackgroundTypes() {
        List<MenuBackgroundType> l = new ArrayList<>();
        l.addAll(backgroundTypes.values());
        return l;
    }

    public static Map<String, MenuBackgroundType> getBackgroundTypesAsMap() {
        return backgroundTypes;
    }

    /**
     * Returns the background type or NULL if no background type with this identifier was found.
     */
    public static MenuBackgroundType getBackgroundTypeByIdentifier(String typeIdentifier) {
        return backgroundTypes.get(typeIdentifier);
    }

    @EventListener
    public void onReload(MenuReloadEvent e) {
        for (MenuBackgroundType t : backgroundTypes.values()) {
            t.loadBackgrounds();
        }
    }

}
