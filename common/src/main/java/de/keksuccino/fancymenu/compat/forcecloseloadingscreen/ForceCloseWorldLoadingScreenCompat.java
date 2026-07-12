package de.keksuccino.fancymenu.compat.forcecloseloadingscreen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jetbrains.annotations.NotNull;

public final class ForceCloseWorldLoadingScreenCompat {

    private static final String TITLE_BRIDGE_SCREEN_CLASS = "eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen";

    private ForceCloseWorldLoadingScreenCompat() {
    }

    @NotNull
    public static Class<? extends Screen> getCompatibleScreenClass(@NotNull Class<? extends Screen> screenClass) {
        // The optional mod replaces TitleScreen with this otherwise behavior-compatible subclass. Avoid linking its class so FancyMenu still loads without the mod.
        if (TITLE_BRIDGE_SCREEN_CLASS.equals(screenClass.getName()) && TitleScreen.class.isAssignableFrom(screenClass)) return TitleScreen.class;
        return screenClass;
    }

    @NotNull
    public static String getCompatibleScreenClassName(@NotNull String screenClassName) {
        // Canonicalize persisted identifiers too; this recovers settings saved while the bridge screen was treated as a separate menu.
        if (TITLE_BRIDGE_SCREEN_CLASS.equals(screenClassName)) return TitleScreen.class.getName();
        return screenClassName;
    }

}
