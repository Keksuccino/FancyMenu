package de.keksuccino.fancymenu.customization.element.container;

/**
 * The scale a container draws one of its children with right now.<br>
 * A container that scales what its children draw does it through the pose of the GUI, around the top-left corner of the child.
 * While such a child renders, it works with places in its own unscaled space. Everything that has to know where such a place is on the screen,
 * like a scissor on a game version that knows nothing about the pose,
 * maps it with this.
 */
public final class ElementContainerDrawScale {

    private static boolean active = false;
    private static float scale = 1.0F;
    private static int originX = 0;
    private static int originY = 0;
    /** If the scissor that gets enabled next is on the screen already, see {@link #markNextScissorAsOnScreen()}. */
    private static boolean nextScissorOnScreen = false;

    private ElementContainerDrawScale() {
    }

    /**
     * Has to be called right before a child renders scaled, and {@link #end()} right after it.
     *
     * @param childX Where the child starts on the screen, which is the point the scale leaves in place.
     */
    public static void begin(float childScale, int childX, int childY) {
        active = true;
        scale = childScale;
        originX = childX;
        originY = childY;
    }

    public static void end() {
        active = false;
        nextScissorOnScreen = false;
    }

    /**
     * Returns if a child renders scaled right now.
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Maps an X position in the unscaled space of the child that renders right now to where it is on the screen.
     */
    public static double toScreenX(double x) {
        return originX + ((x - originX) * scale);
    }

    /** The Y counterpart of {@link #toScreenX(double)}. */
    public static double toScreenY(double y) {
        return originY + ((y - originY) * scale);
    }

    /**
     * Tells that the scissor that gets enabled next got mapped to the screen by what enables it, so it does not get mapped again.
     */
    public static void markNextScissorAsOnScreen() {
        if (active) nextScissorOnScreen = true;
    }

    /**
     * Returns if a scissor that gets enabled right now has to be mapped to the screen, and forgets what got told about it.
     */
    public static boolean mapsNextScissor() {
        boolean maps = active && !nextScissorOnScreen;
        nextScissorOnScreen = false;
        return maps;
    }

}
