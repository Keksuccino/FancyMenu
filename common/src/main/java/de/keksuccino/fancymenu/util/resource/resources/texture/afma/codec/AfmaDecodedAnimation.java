package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Exact decoded AFMA animation used as the interchange model between the
 * creator pipeline, the production codec, and the runtime decoder.
 */
public record AfmaDecodedAnimation(
        int width,
        int height,
        @NotNull List<AfmaDecodedFrame> introFrames,
        @NotNull List<AfmaDecodedFrame> mainFrames
) {

    public AfmaDecodedAnimation {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Decoded animation dimensions must be positive");
        }
        introFrames = List.copyOf(Objects.requireNonNull(introFrames, "introFrames"));
        mainFrames = List.copyOf(Objects.requireNonNull(mainFrames, "mainFrames"));
    }
}
