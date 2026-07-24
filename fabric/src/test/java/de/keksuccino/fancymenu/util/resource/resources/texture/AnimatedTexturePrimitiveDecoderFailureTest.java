package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedTexturePrimitiveDecoderFailureTest {

    private static final byte[] INVALID_IMAGE_DATA = new byte[0];

    @Test
    void activeGifDecodeFailurePublishesTerminalState() {
        GifTexture texture = new GifTexture();

        GifTexture.populateTextureWithPrimitiveDecoder(texture, INVALID_IMAGE_DATA, "invalid.gif");

        assertTrue(texture.loadingFailed.get());
        assertTrue(texture.decoded.get());
    }

    @Test
    void closedGifGenerationCannotPublishDecodeFailure() {
        GifTexture texture = new GifTexture();
        texture.frameStore.close();

        GifTexture.populateTextureWithPrimitiveDecoder(texture, INVALID_IMAGE_DATA, "invalid.gif");

        assertFalse(texture.loadingFailed.get());
        assertFalse(texture.decoded.get());
    }

    @Test
    void activeApngDecodeFailurePublishesTerminalState() {
        ApngTexture texture = new ApngTexture();

        ApngTexture.populateTextureWithPrimitiveDecoder(texture, INVALID_IMAGE_DATA, "invalid.png");

        assertTrue(texture.loadingFailed.get());
        assertTrue(texture.decoded.get());
    }

    @Test
    void closedApngGenerationCannotPublishDecodeFailure() {
        ApngTexture texture = new ApngTexture();
        texture.frameStore.close();

        ApngTexture.populateTextureWithPrimitiveDecoder(texture, INVALID_IMAGE_DATA, "invalid.png");

        assertFalse(texture.loadingFailed.get());
        assertFalse(texture.decoded.get());
    }

}
