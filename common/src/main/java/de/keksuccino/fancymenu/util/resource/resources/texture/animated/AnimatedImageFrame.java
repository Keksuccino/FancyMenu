package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

/**
 * One frame of an animated image: the canvas region it covers, how long it stays visible and how it gets composed onto the canvas.<br>
 * The region always lies inside the canvas. Formats whose frames can reach past the canvas, like GIF, clip their frames to it.
 * Regions can be empty (zero width or height), then the frame only shows the canvas for its delay.
 */
public interface AnimatedImageFrame {

    /** The frame stays on the canvas as it is when the next frame gets composed. **/
    int DISPOSE_OP_NONE = 0;
    /** The frame region gets cleared to fully transparent black before the next frame gets composed. **/
    int DISPOSE_OP_BACKGROUND = 1;
    /** The frame region gets reverted to what it showed before this frame got composed. **/
    int DISPOSE_OP_PREVIOUS = 2;
    /** The frame replaces all pixels of its region, alpha included. **/
    int BLEND_OP_SOURCE = 0;
    /** The frame gets alpha-composited over the pixels of its region. **/
    int BLEND_OP_OVER = 1;

    int x();

    int y();

    int width();

    int height();

    /**
     * How long the frame stays visible, as defined by its format. Playback shows every frame for at least {@link AnimatedFrameStream#MIN_FRAME_DELAY_NANOS}.
     */
    long delayNanos();

    /**
     * What happens to the frame region before the next frame gets composed, one of the DISPOSE_OP constants.
     */
    int disposeOp();

    /**
     * How the frame gets composed onto the canvas, one of the BLEND_OP constants.
     */
    int blendOp();

}
