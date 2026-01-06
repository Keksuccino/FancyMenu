package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenOverlays {

    public static final long PLACEHOLDER_1 = register(null, false);
    public static final long PLACEHOLDER_2 = register(null, false);
    public static final long PLACEHOLDER_3 = register(null, false);
    public static final long PLACEHOLDER_4 = register(null, false);
    public static final long PLACEHOLDER_5 = register(null, false);
    public static final long PLACEHOLDER_6 = register(null, false);
    public static final long PLACEHOLDER_7 = register(null, false);
    public static final long PLACEHOLDER_8 = register(null, false);
    public static final long PLACEHOLDER_9 = register(null, false);
    public static final long PLACEHOLDER_10 = register(null, false);

    public static final long PIP_WINDOW_HANDLER = register(PiPWindowHandler.INSTANCE, false);
    public static final long CUSTOMIZATION_DEBUG_OVERLAY = register(null, true);
    public static final long CUSTOMIZATION_MENU_BAR = register(null, true);
    public static final long LAYOUT_EDITOR_MENU_BAR = register(null, true);
    public static final long LAYOUT_EDITOR_RIGHT_CLICK_CONTEXT_MENU = register(null, true);
    public static final long LAYOUT_EDITOR_ELEMENT_CONTEXT_MENU = register(null, true);

    public static void registerDefaults() {
        // empty dummy just for having something to call on init (yes, it's useless, but it makes me feel better, okay?)
    }

    private static long register(@Nullable Renderable overlay, boolean setDefaultInputController) {
        long id;
        if (overlay != null) {
            id = ScreenOverlayHandler.INSTANCE.addOverlay(overlay);
        } else {
            id = ScreenOverlayHandler.INSTANCE.addPlaceholder();
        }
        if (setDefaultInputController) {
            ScreenOverlayHandler.INSTANCE.setInputConsumptionControllerFor(id, screen -> !PiPWindowHandler.INSTANCE.isForceFocusWindowOpen());
        }
        return id;
    }

}
