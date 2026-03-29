package de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Shared contract for AFMA animation codecs that compress and restore exact
 * decoded frame sequences.
 */
public interface AfmaAnimationCodec {

    /**
     * Returns a stable codec identifier for diagnostics and metadata.
     */
    @NotNull String name();

    /**
     * Compresses a fully decoded animation into the codec-specific binary stream.
     */
    byte[] compress(@NotNull AfmaDecodedAnimation animation) throws IOException;

    /**
     * Restores the exact decoded animation from the codec-specific binary stream.
     */
    @NotNull AfmaDecodedAnimation decompress(@NotNull String animationName, byte[] compressedBytes) throws IOException;
}
