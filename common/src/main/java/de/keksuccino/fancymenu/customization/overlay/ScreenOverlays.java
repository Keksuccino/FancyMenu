package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;

public class ScreenOverlays {

    public static final long PLACEHOLDER_1 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_2 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_3 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_4 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_5 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_6 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_7 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_8 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_9 = ScreenOverlayHandler.INSTANCE.addPlaceholder();
    public static final long PLACEHOLDER_10 = ScreenOverlayHandler.INSTANCE.addPlaceholder();

    public static final long PIP_WINDOW_HANDLER = ScreenOverlayHandler.INSTANCE.addOverlay(PiPWindowHandler.INSTANCE);
    public static final long CUSTOMIZATION_DEBUG_OVERLAY = CustomizationOverlay.refreshDebugOverlay();
    public static final long CUSTOMIZATION_MENU_BAR = CustomizationOverlay.refreshMenuBar();

    public static void registerDefaults() {
        // empty dummy just for having something to call on init (yes, it's useless, but it makes me feel better, okay?)
    }

}
