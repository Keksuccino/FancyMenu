package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;

/**
 * A parsed animated image, like an APNG or GIF image: its canvas size, how often it plays, its frames and the source its frames read their compressed data from.<br>
 * Parsed images keep no pixels, so they need only a few bytes per frame, no matter how big their file is. Frames get decoded from the source again whenever they get shown.<br>
 * Still images (and animations that broke before their first intact frame) have a single frame and are not animated.
 * The image owns its source, so closing the image closes the source.
 */
public interface AnimatedImage extends AutoCloseable {

    /** Number of plays of animations that loop forever. **/
    int INFINITE_PLAYS = 0;
    /** Largest supported canvas width and height. Most GPUs can't use bigger textures. **/
    int MAX_DIMENSION = 16384;
    /** Largest supported canvas pixel count, which keeps the 32-bit canvas of an image at 256 MB or less. **/
    long MAX_PIXEL_COUNT = 1L << 26;

    /**
     * Name of the image format, like "APNG" or "GIF", for log messages.
     */
    @NotNull
    String getFormatName();

    int getWidth();

    int getHeight();

    /**
     * If the image is an animation. Still images only have a single frame.
     */
    boolean isAnimated();

    /**
     * How often the animation plays, or {@link #INFINITE_PLAYS} if it loops forever. Only matters for animated images.
     */
    int getNumPlays();

    /**
     * How many frames the image has, always at least one.
     */
    int getFrameCount();

    @NotNull
    AnimatedImageFrame getFrame(int index);

    /**
     * Creates a decoder for the frames of the image. A decoder is not thread-safe, so every thread that decodes frames needs its own decoder.
     */
    @NotNull
    AnimatedFrameDecoder createFrameDecoder();

    /**
     * Closes the source. Frames can't get decoded anymore afterwards.
     */
    @Override
    void close();

}
