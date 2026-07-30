package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.platform.Services;
import java.util.function.Predicate;

public class RinkuUtil {

    public static volatile boolean RINKU_critical_failure = false;
    public static volatile boolean rinku_initialized = false;

    public static boolean isRinkuLoaded() {
        if (RINKU_critical_failure) return false;
        if (FancyMenu.getOptions().devForceRinkuMissing.getValue()) return false;
        return isRinkuPresent();
    }

    /**
     * Checks only whether Rinku's mod ID is loaded. Shutdown cleanup must ignore runtime availability overrides because they can change after Rinku resources were created.
     */
    public static boolean isRinkuPresent() {
        return isRinkuPresent(Services.PLATFORM::isModLoaded);
    }

    static boolean isRinkuPresent(Predicate<String> isModLoaded) {
        return isModLoaded.test(Rinku.MOD_ID);
    }

}
