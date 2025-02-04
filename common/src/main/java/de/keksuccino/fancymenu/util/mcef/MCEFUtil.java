package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.FancyMenu;

public class MCEFUtil {

    public static boolean isMCEFLoaded() {
        try {
            Class.forName("com.cinemamod.mcef.MCEF", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

}
