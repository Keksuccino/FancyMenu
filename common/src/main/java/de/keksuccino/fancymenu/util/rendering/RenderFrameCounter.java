package de.keksuccino.fancymenu.util.rendering;

/**
 * Counts the frames the game renders. Things that have to know if something else got rendered in the frame before
 * compare the frames they remembered, which works at every frame rate, unlike comparing times.<br>
 * The count goes up once per frame, but screens can put together what they show before or after that happens in a frame.
 * So something that was part of the last frame is zero or one count behind, and everything older is further behind.
 */
public final class RenderFrameCounter {

    private static long frame = 0L;

    private RenderFrameCounter() {
    }

    /**
     * Returns the count of frames right now.
     */
    public static long getFrame() {
        return frame;
    }

    /**
     * Counts the next frame. Gets called once per frame.
     */
    public static void nextFrame() {
        frame++;
    }

}
