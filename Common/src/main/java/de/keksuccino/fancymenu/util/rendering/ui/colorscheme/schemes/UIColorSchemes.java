package de.keksuccino.fancymenu.util.rendering.ui.colorscheme.schemes;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.ui.colorscheme.UIColorSchemeRegistry;

public class UIColorSchemes {

    public static final DarkUIColorScheme DARK = new DarkUIColorScheme();
    public static final LightUIColorScheme LIGHT = new LightUIColorScheme();

    public static void registerAll() {

        UIColorSchemeRegistry.register("dark", DARK);
        UIColorSchemeRegistry.register("light", LIGHT);

        updateActiveScheme();

    }

    public static boolean isLightMode() {
        return UIColorSchemeRegistry.getActiveScheme() == LIGHT;
    }

    public static void updateActiveScheme() {
        if (FancyMenu.getConfig().getOrDefault("light_mode", false)) {
            UIColorSchemeRegistry.setActiveScheme("light");
        } else {
            UIColorSchemeRegistry.setActiveScheme("dark");
        }
    }

}
