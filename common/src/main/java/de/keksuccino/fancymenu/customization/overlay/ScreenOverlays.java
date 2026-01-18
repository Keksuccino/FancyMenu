package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
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

    public static final long PLACEHOLDER_11 = register(null, false);
    public static final long PLACEHOLDER_12 = register(null, false);
    public static final long PLACEHOLDER_13 = register(null, false);
    public static final long PLACEHOLDER_14 = register(null, false);
    public static final long PLACEHOLDER_15 = register(null, false);
    public static final long PLACEHOLDER_16 = register(null, false);
    public static final long PLACEHOLDER_17 = register(null, false);
    public static final long PLACEHOLDER_18 = register(null, false);
    public static final long PLACEHOLDER_19 = register(null, false);
    public static final long PLACEHOLDER_20 = register(null, false);

    public static final long CONTEXT_MENU_HANDLER = register(ContextMenuHandler.INSTANCE, false);

    public static final long PLACEHOLDER_21 = register(null, false);
    public static final long PLACEHOLDER_22 = register(null, false);
    public static final long PLACEHOLDER_23 = register(null, false);
    public static final long PLACEHOLDER_24 = register(null, false);
    public static final long PLACEHOLDER_25 = register(null, false);
    public static final long PLACEHOLDER_26 = register(null, false);
    public static final long PLACEHOLDER_27 = register(null, false);
    public static final long PLACEHOLDER_28 = register(null, false);
    public static final long PLACEHOLDER_29 = register(null, false);
    public static final long PLACEHOLDER_30 = register(null, false);

    public static final long TOOLTIPS = register(TooltipHandler.INSTANCE, false);

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
