package de.keksuccino.fancymenu.compat;

import de.keksuccino.fancymenu.compat.forcecloseloadingscreen.ForceCloseWorldLoadingScreenCompat;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public final class ScreenCompatibility {

    private ScreenCompatibility() {
    }

    /**
     * Returns the logical screen class FancyMenu should use for layouts and runtime state.
     * Compatibility aliases must stay exact so unrelated subclasses remain independently customizable.
     */
    @NotNull
    public static Class<? extends Screen> getCompatibleScreenClass(@NotNull Class<? extends Screen> screenClass) {
        return ForceCloseWorldLoadingScreenCompat.getCompatibleScreenClass(screenClass);
    }

    @NotNull
    public static String getCompatibleScreenClassName(@NotNull String screenClassName) {
        return ForceCloseWorldLoadingScreenCompat.getCompatibleScreenClassName(screenClassName);
    }

}
