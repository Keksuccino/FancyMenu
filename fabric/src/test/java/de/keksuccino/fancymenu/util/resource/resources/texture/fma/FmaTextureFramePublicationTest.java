package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FmaTextureFramePublicationTest {

    @Test
    void publishesCurrentGenerationFrameAndPlaybackStateTogether() {
        FmaTexture texture = new FmaTexture();
        try {
            int generation = texture.streamGeneration.get();
            FmaTexture.DecodedFrame frame = new FmaTexture.DecodedFrame(false, 3, 25L, new NativeImage(1, 1, true));

            assertTrue(texture.publishDecodedFrame(frame, generation, 123L));
            assertSame(frame, texture.pendingUploadFrame.get());
            assertTrue(texture.playbackInitialized);
            assertFalse(texture.playbackIntro);
            assertEquals(3, texture.playbackIndex);
            assertEquals(123L, texture.playbackFrameStartMs);
            assertEquals(25L, texture.playbackFrameDelayMs);
        } finally {
            texture.close();
        }
    }

    @Test
    void rejectsFramePublishedByGenerationThatWasReset() {
        FmaTexture texture = new FmaTexture();
        try {
            int staleGeneration = texture.streamGeneration.get();
            texture.requestPlaybackReset();

            FmaTexture.DecodedFrame staleFrame = new FmaTexture.DecodedFrame(false, 0, 10L, new NativeImage(1, 1, true));
            assertFalse(texture.publishDecodedFrame(staleFrame, staleGeneration, 123L));
            assertNull(staleFrame.nativeImage);
            assertNull(texture.pendingUploadFrame.get());
            assertFalse(texture.playbackInitialized);
        } finally {
            texture.close();
        }
    }

}
