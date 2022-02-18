package de.keksuccino.fancymenu.api.background;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuBackgroundTypeRegistry {

    protected static Map<String, MenuBackgroundType> backgroundTypes = new HashMap<>();

    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(new MenuBackgroundTypeRegistry());
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
                    FancyMenu.LOGGER.warn("[FANCYMENU] WARNING! A menu background type with the identifier '" + type.getIdentifier() + "' is already registered! Overriding type!");
                }
                backgroundTypes.put(type.getIdentifier(), type);
                type.loadBackgrounds();
            } else {
                FancyMenu.LOGGER.error("[FANCYMENU] ERROR! Identifier cannot be null for MenuBackgroundTypes!");
            }
        } else {
            FancyMenu.LOGGER.error("[FANCYMENU] ERROR: registerBackgroundType: Menu background type cannot be null!");
        }
    }

    /**
     * Unregister a previously added menu background type.
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

    @SubscribeEvent
    public void onReload(MenuReloadedEvent e) {
        for (MenuBackgroundType t : backgroundTypes.values()) {
            t.loadBackgrounds();
        }
    }

}
