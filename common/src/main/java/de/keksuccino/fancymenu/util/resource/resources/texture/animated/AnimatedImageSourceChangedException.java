package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;

/**
 * The source of an animated image doesn't hold the data anymore that got parsed from it, because its file got changed or shortened, or can't get read anymore.<br>
 * Unlike corrupted frame data, this is no problem of a single frame, so the image can't play any further frames.
 */
public class AnimatedImageSourceChangedException extends IOException {

    public AnimatedImageSourceChangedException(@NotNull String message) {
        super(message);
    }

    public AnimatedImageSourceChangedException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}
