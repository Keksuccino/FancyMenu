package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Immutable decoded frame payload used by the AFMA production codec.
 */
public record AfmaDecodedFrame(int width, int height, @NotNull int[] argbPixels) {

    public AfmaDecodedFrame {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Decoded frame dimensions must be positive");
        }
        if (argbPixels.length != (width * height)) {
            throw new IllegalArgumentException("Decoded frame pixel buffer size does not match the dimensions");
        }
        argbPixels = Arrays.copyOf(argbPixels, argbPixels.length);
    }

    /**
     * Returns a defensive copy of the stored pixels so callers can render or mutate
     * the result without changing the cached decoded frame.
     */
    public @NotNull int[] copyPixels() {
        return Arrays.copyOf(this.argbPixels, this.argbPixels.length);
    }
}
