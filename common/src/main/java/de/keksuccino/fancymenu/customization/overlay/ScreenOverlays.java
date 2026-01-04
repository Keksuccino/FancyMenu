package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;

public class ScreenOverlays {

    public static void registerDefaults() {

        ScreenOverlayHandler.INSTANCE.addOverlay(PiPWindowHandler.INSTANCE);

        CustomizationOverlay.refreshDebugOverlay(null, true); // This calls the addOverlay method to register the debug overlay

        CustomizationOverlay.refreshMenuBar(null, true); // This calls the addOverlay method to register the menu bar

    }

}
