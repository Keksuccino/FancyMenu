package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FmaTextureGenerationTest {

    @Test
    void staleGenerationDoesNotConsumeNewStartEvent() {
        FmaTexture texture = new FmaTexture();
        texture.streamGeneration.set(2);
        texture.pendingStartEvent = true;

        texture.maybeEmitStartEvent(false, 0, 1);

        assertTrue(texture.pendingStartEvent);
    }

    @Test
    void staleGenerationDoesNotRestoreTerminalCycleStateAfterReset() {
        FmaTexture texture = new FmaTexture();
        texture.streamGeneration.set(2);
        texture.numPlays.set(1);
        texture.cycles.set(0);
        texture.playRequested = true;
        texture.maxLoopsReached = false;

        FmaTexture.CycleBoundaryResult result = texture.handleCycleBoundary(1);

        assertEquals(FmaTexture.CycleBoundaryResult.STALE, result);
        assertEquals(0, texture.cycles.get());
        assertTrue(texture.playRequested);
        assertFalse(texture.maxLoopsReached);
    }

    @Test
    void staleGenerationDoesNotConsumeNewPrefetchedFrame() {
        FmaTexture texture = new FmaTexture();
        texture.streamGeneration.set(2);
        FmaTexture.DecodedFrame frame = new FmaTexture.DecodedFrame(false, 0, 10L, new NativeImage(1, 1, true));
        texture.prefetchedFrames.add(frame);

        try {
            assertNull(texture.pollPrefetchedFrame(1));
            assertSame(frame, texture.prefetchedFrames.peekFirst());
        } finally {
            texture.clearPrefetchedFramesLocked();
        }
    }

    @Test
    void currentGenerationConsumesPrefetchedFrameNormally() {
        FmaTexture texture = new FmaTexture();
        texture.streamGeneration.set(2);
        FmaTexture.DecodedFrame frame = new FmaTexture.DecodedFrame(false, 0, 10L, new NativeImage(1, 1, true));
        texture.prefetchedFrames.add(frame);

        try {
            assertSame(frame, texture.pollPrefetchedFrame(2));
            assertTrue(texture.prefetchedFrames.isEmpty());
        } finally {
            frame.close();
            texture.clearPrefetchedFramesLocked();
        }
    }

}
