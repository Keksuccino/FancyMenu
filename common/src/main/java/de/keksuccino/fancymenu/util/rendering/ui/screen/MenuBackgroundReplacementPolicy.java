package de.keksuccino.fancymenu.util.rendering.ui.screen;

/**
 * Defines when legacy list screens need the global menu background as their base surface.
 */
public final class MenuBackgroundReplacementPolicy {

    private MenuBackgroundReplacementPolicy() {
    }

    public static boolean shouldRenderGlobalBase(boolean hasScreenMenuBackgrounds) {
        return !hasScreenMenuBackgrounds;
    }

    public static boolean shouldRenderListBase(boolean inWorld, boolean hasScreenMenuBackgrounds) {
        return !inWorld && shouldRenderGlobalBase(hasScreenMenuBackgrounds);
    }

    public static boolean shouldRenderLegacyBoundedPanel(boolean replacementRendered, boolean hasScreenMenuBackgrounds) {
        return !replacementRendered && !hasScreenMenuBackgrounds;
    }

}
