package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.jetbrains.annotations.NotNull;

/**
 * One frame of an APNG image: the canvas region it covers, how long it stays visible, how it gets composed onto the canvas and its compressed image data.<br>
 * Frames only keep their zlib-compressed data, so a parsed image needs about as much memory as its file.
 *
 * @param delayNanos How long the frame stays visible, exactly as stored in the file.
 * @param data The zlib stream of the frame, with the chunk structure of the file already removed.
 */
public record ApngFrame(int x, int y, int width, int height, long delayNanos, int disposeOp, int blendOp, @NotNull byte[] data) {

    /** The frame stays on the canvas as it is when the next frame gets composed. **/
    public static final int DISPOSE_OP_NONE = 0;
    /** The frame region gets cleared to fully transparent black before the next frame gets composed. **/
    public static final int DISPOSE_OP_BACKGROUND = 1;
    /** The frame region gets reverted to what it showed before this frame got composed. **/
    public static final int DISPOSE_OP_PREVIOUS = 2;
    /** The frame replaces all pixels of its region, alpha included. **/
    public static final int BLEND_OP_SOURCE = 0;
    /** The frame gets alpha-composited over the pixels of its region. **/
    public static final int BLEND_OP_OVER = 1;

    public boolean coversCanvas(int canvasWidth, int canvasHeight) {
        return (this.x == 0) && (this.y == 0) && (this.width == canvasWidth) && (this.height == canvasHeight);
    }

}
