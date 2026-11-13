package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

/**
 * Converts between the two sizes the body of a {@link PiPWindow} has: the size of the area the window leaves for it, in whole GUI pixels,
 * and the size of the screen that area shows, in the coordinates of that screen.<br>
 * The screen gets drawn with a render scale of its own, and both sizes are whole numbers, so one of them can't simply get multiplied
 * to get the other one: a screen is always big enough to fill its area, which makes it up to one unit bigger than the area needs.
 */
final class PiPWindowBodySizing {

    private PiPWindowBodySizing() {
    }

    /**
     * Returns the size of the screen that fills a body of the given size.
     *
     * @param bodySize The size of the body in GUI pixels.
     * @param renderScale How many GUI pixels one unit of the screen covers.
     */
    static int screenSize(int bodySize, double renderScale) {
        double scaledSize = (double) bodySize / renderScale;
        int result = (int) scaledSize;
        if (scaledSize > (double) result) {
            result++;
        }
        return Math.max(1, result);
    }

    /**
     * Returns the smallest size of a body whose screen is at least as big as given.
     *
     * @param screenSize The size the screen has to have at least, in its own coordinates.
     * @param renderScale How many GUI pixels one unit of the screen covers.
     */
    static int bodySizeForScreenSize(int screenSize, double renderScale) {
        int bodySize = Math.max(1, (int) Math.floor((screenSize - 1) * renderScale) + 1);
        // Both steps make up for what the multiplication loses to rounding
        while ((bodySize > 1) && (screenSize(bodySize - 1, renderScale) >= screenSize)) bodySize--;
        while (screenSize(bodySize, renderScale) < screenSize) bodySize++;
        return bodySize;
    }

}
