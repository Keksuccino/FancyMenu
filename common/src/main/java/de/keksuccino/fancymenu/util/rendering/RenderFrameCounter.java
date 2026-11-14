package de.keksuccino.fancymenu.util.rendering;

/**
 * Counts the frames the game renders. Things that have to know if something else got rendered in the frame before
 * compare the frames they remembered, which works at every frame rate, unlike comparing times.
 */
public final class RenderFrameCounter {

    private static long frame = 0L;

    private RenderFrameCounter() {
    }

    /**
     * Returns the number of the frame the game renders right now, or rendered last.
     */
    public static long getFrame() {
        return frame;
    }

    /**
     * Starts the next frame. Gets called once per frame, before the game renders anything of it.
     */
    public static void nextFrame() {
        frame++;
    }

}
