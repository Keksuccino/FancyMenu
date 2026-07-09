package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.FancyMenu;

public class MCEFUtil {

    public static volatile boolean MCEF_critical_failure = false;
    public static volatile boolean MCEF_initialized = false;

    public static boolean isMCEFLoaded() {
        if (MCEF_critical_failure) return false;
        if (FancyMenu.getOptions().devForceMcefMissing.getValue()) return false;
        return isMCEFPresent();
    }

    /**
     * Checks only whether MCEF classes are present. Shutdown cleanup must ignore runtime availability overrides because they can change after MCEF resources were created.
     */
    public static boolean isMCEFPresent() {
        try {
            Class.forName("com.cinemamod.mcef.MCEF", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception | LinkageError ignored) {}
        return false;
    }

}
