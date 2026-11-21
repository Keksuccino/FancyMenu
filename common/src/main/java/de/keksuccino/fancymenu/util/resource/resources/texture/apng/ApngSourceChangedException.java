package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;

/**
 * The source of an APNG image doesn't hold the data anymore that got parsed from it, because its file got changed or shortened, or can't get read anymore.<br>
 * Unlike corrupted frame data, this is no problem of a single frame, so the image can't play any further frames.
 */
public class ApngSourceChangedException extends IOException {

    public ApngSourceChangedException(@NotNull String message) {
        super(message);
    }

    public ApngSourceChangedException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}
