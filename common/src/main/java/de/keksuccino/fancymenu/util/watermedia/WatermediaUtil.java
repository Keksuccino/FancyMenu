package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;

public class WatermediaUtil {

    public static volatile boolean WATERMEDIA_critical_failure = false;
    public static volatile boolean WATERMEDIA_initialized = false;

    public static boolean isWatermediaLoaded() {
        if (WATERMEDIA_critical_failure) return false;
        try {
            Class.forName("org.watermedia.api.media.MRL", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

}
