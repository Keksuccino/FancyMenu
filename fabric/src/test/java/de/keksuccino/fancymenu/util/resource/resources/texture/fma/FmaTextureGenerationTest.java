package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that streams of older generations, which a reset replaced while they still run, change nothing of the playback of the reset.
 */
class FmaTextureGenerationTest {

    @Test
    void publishesFrameAndPlaybackStateOfTheCurrentGenerationTogether() {
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
    void staleGenerationDoesntPublishItsFrame() {
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

    @Test
    void staleGenerationDoesntTakeTheStartEventOfTheReset() {
        FmaTexture texture = new FmaTexture();
        texture.streamGeneration.set(2);
        texture.pendingStartEvent = true;

        texture.maybeEmitStartEvent(false, 0, 1);

        assertTrue(texture.pendingStartEvent);
    }

    @Test
    void staleGenerationDoesntCountACycleOrStopThePlaybackAfterAReset() {
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
    void streamThatStopsAtItsLastCycleRightBeforeAResetDoesntStopThePlaybackOfTheReset() {
        StaleStopTexture texture = new StaleStopTexture();
        texture.numPlays.set(1);

        texture.reset();

        assertTrue(texture.stoppedBeforeReset);
        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
    }

    @Test
    void staleGenerationDoesntTakeAFrameOfTheNewGeneration() {
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
    void currentGenerationTakesItsFrames() {
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

    /**
     * A texture whose stream finishes its last play cycle right before a reset takes the stream state lock, which is the latest moment it still can.
     */
    private static final class StaleStopTexture extends FmaTexture {

        private boolean stoppedBeforeReset;

        @Override
        protected void requestPlaybackReset() {
            this.stoppedBeforeReset = this.handleCycleBoundary(this.streamGeneration.get()) == CycleBoundaryResult.STOP;
            super.requestPlaybackReset();
        }

        @Override
        protected void startTickerIfNeeded() {
        }

    }

}
