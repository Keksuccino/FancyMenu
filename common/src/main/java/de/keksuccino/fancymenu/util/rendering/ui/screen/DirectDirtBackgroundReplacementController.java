package de.keksuccino.fancymenu.util.rendering.ui.screen;

import java.util.function.BooleanSupplier;

/**
 * Selects the replacement for legacy screens that invoke {@code Screen.renderDirtBackground} directly.
 */
public final class DirectDirtBackgroundReplacementController {

    private DirectDirtBackgroundReplacementController() {
    }

    /**
     * Attempts the screen layout before the global fallback. Wrapped dirt calls are owned by
     * {@code Screen.renderBackground} and must remain untouched to avoid duplicate background events.
     */
    public static boolean renderReplacement(boolean wrappedDirtCall, BooleanSupplier screenReplacement, BooleanSupplier globalReplacement) {
        if (wrappedDirtCall) return false;
        if (screenReplacement.getAsBoolean()) return true;
        return globalReplacement.getAsBoolean();
    }

}
