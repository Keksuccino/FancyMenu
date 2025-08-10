package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.FancyMenu;

public class MCEFUtil {

    public static volatile boolean MCEF_critical_failure = false;
    public static volatile boolean MCEF_initialized = false;

    public static boolean isMCEFLoaded() {
        if (MCEF_critical_failure) return false;
        try {
            Class.forName("com.cinemamod.mcef.MCEF", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

}
