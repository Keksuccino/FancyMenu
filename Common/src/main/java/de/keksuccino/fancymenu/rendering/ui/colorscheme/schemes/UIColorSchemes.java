package de.keksuccino.fancymenu.rendering.ui.colorscheme.schemes;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.rendering.ui.colorscheme.UIColorSchemeRegistry;

public class UIColorSchemes {

    public static final DarkUIColorScheme DARK = new DarkUIColorScheme();
    public static final LightUIColorScheme LIGHT = new LightUIColorScheme();

    private boolean lastLightModeState = FancyMenu.getConfig().getOrDefault("light_mode", false);

    public static void registerAll() {

        UIColorSchemeRegistry.register("dark", DARK);
        UIColorSchemeRegistry.register("light", LIGHT);

        updateActiveScheme();

        EventHandler.INSTANCE.registerListenersOf(new UIColorSchemes());

    }

    public static boolean isLightMode() {
        return UIColorSchemeRegistry.getActiveScheme() == LIGHT;
    }

    private static void updateActiveScheme() {
        if (FancyMenu.getConfig().getOrDefault("light_mode", false)) {
            UIColorSchemeRegistry.setActiveScheme("light");
        } else {
            UIColorSchemeRegistry.setActiveScheme("dark");
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        boolean b = FancyMenu.getConfig().getOrDefault("light_mode", false);
        if (b != lastLightModeState) {
            updateActiveScheme();
        }
        lastLightModeState = b;
    }

}
