package de.keksuccino.fancymenu.util.rendering;

import java.util.Objects;

/**
 * Coordinates full-screen menu-background replacement around Screen's virtual background lifecycle.
 */
public final class MenuBackgroundLifecycleController {

    private static final ThreadLocal<Integer> STATIC_HELPER_SUPPRESSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    private int wrappedCallDepth;

    public void renderLifecycle(int screenWidth, int screenHeight, FullScreenRenderer seamlessRenderer, FullScreenRenderer globalRenderer, Runnable fallbackRenderer) {
        Objects.requireNonNull(seamlessRenderer);
        Objects.requireNonNull(globalRenderer);
        Objects.requireNonNull(fallbackRenderer);
        if (renderReplacement(screenWidth, screenHeight, seamlessRenderer, globalRenderer)) return;

        this.wrappedCallDepth++;
        int previousSuppressionDepth = beginStaticHelperSuppression();
        try {
            fallbackRenderer.run();
        } finally {
            // The fallback is a virtual call and may dispatch to an override or re-enter through a PiP render.
            // Restore both guards exactly so an exception or nested render cannot suppress a later replacement.
            endStaticHelperSuppression(previousSuppressionDepth);
            this.wrappedCallDepth--;
        }
    }

    public boolean renderDirectOneArgumentCall(int screenWidth, int screenHeight, FullScreenRenderer seamlessRenderer, FullScreenRenderer globalRenderer) {
        Objects.requireNonNull(seamlessRenderer);
        Objects.requireNonNull(globalRenderer);
        return this.wrappedCallDepth == 0 && renderReplacement(screenWidth, screenHeight, seamlessRenderer, globalRenderer);
    }

    public void renderNestedBoundedCall(Runnable renderer) {
        Objects.requireNonNull(renderer);
        int previousSuppressionDepth = beginStaticHelperSuppression();
        try {
            renderer.run();
        } finally {
            endStaticHelperSuppression(previousSuppressionDepth);
        }
    }

    public static boolean isStaticHelperReplacementAllowed() {
        return STATIC_HELPER_SUPPRESSION_DEPTH.get() == 0;
    }

    private static boolean renderReplacement(int screenWidth, int screenHeight, FullScreenRenderer seamlessRenderer, FullScreenRenderer globalRenderer) {
        return seamlessRenderer.render(0, 0, screenWidth, screenHeight) || globalRenderer.render(0, 0, screenWidth, screenHeight);
    }

    private static int beginStaticHelperSuppression() {
        int previousDepth = STATIC_HELPER_SUPPRESSION_DEPTH.get();
        STATIC_HELPER_SUPPRESSION_DEPTH.set(previousDepth + 1);
        return previousDepth;
    }

    private static void endStaticHelperSuppression(int previousDepth) {
        if (previousDepth == 0) {
            STATIC_HELPER_SUPPRESSION_DEPTH.remove();
        } else {
            STATIC_HELPER_SUPPRESSION_DEPTH.set(previousDepth);
        }
    }

    @FunctionalInterface
    public interface FullScreenRenderer {

        boolean render(int x, int y, int width, int height);
    }
}
