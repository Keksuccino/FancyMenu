package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaTextureInactivityTest {

    private final InMemoryAfmaTexture texture = new InMemoryAfmaTexture();

    @AfterEach
    void closeTexture() {
        this.texture.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void retainsEveryPendingFrameAcrossRepeatedInactivity(int pendingFrames) {
        this.iterate(1000L);
        if (pendingFrames == 1) {
            AfmaTexture.PreparedFrame displayed = this.texture.pollPrefetchedFrame();
            assertNotNull(displayed);
            this.texture.playbackIndex = displayed.index;
            displayed.close();
        }
        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        int nextDecodeIndex = this.texture.decodeIndex;

        this.iterate(11001L);
        this.iterate(60000L);

        assertEquals(pending, List.copyOf(this.texture.prefetchedFrames));
        assertEquals(nextDecodeIndex, this.texture.decodeIndex);
        assertEquals(2, this.texture.preparedFrameCount);
        for (AfmaTexture.PreparedFrame frame : pending) {
            assertNotNull(frame.primaryPayload);
            assertNotNull(frame.patchPayload);
        }
    }

    @Test
    void resumesWithEveryDependentFrameInOrder() {
        this.iterate(1000L);
        this.iterate(11001L);
        this.texture.lastResourceLocationCall = 12000L;

        for (int expectedIndex = 21; expectedIndex <= 25; expectedIndex++) {
            this.iterate(12000L);
            AfmaTexture.PreparedFrame next = this.texture.pollPrefetchedFrame();
            assertNotNull(next);
            assertEquals(expectedIndex, next.index);
            assertFalse(next.intro);
            assertNotNull(next.primaryPayload);
            next.close();
        }
    }

    @Test
    void inactiveEmptyQueueWaitsForRenderingBeforeDecodingMoreFrames() {
        this.iterate(11001L);
        this.iterate(60000L);

        assertTrue(this.texture.prefetchedFrames.isEmpty());
        assertEquals(21, this.texture.decodeIndex);
        assertEquals(0, this.texture.preparedFrameCount);

        this.texture.lastResourceLocationCall = 60000L;
        this.iterate(60000L);

        assertEquals(2, this.texture.prefetchedFrames.size());
        assertEquals(21, this.texture.prefetchedFrames.getFirst().index);
    }

    @ParameterizedTest
    @ValueSource(longs = {10999L, 11000L})
    void continuesPrefetchingUntilInactivityTimeoutIsExceeded(long now) {
        this.iterate(now);

        assertEquals(2, this.texture.prefetchedFrames.size());
        assertEquals(23, this.texture.decodeIndex);
    }

    @Test
    void preloadsFramesBeforeTheTextureHasEverBeenRendered() {
        this.texture.lastResourceLocationCall = -1L;

        this.iterate(60000L);

        assertEquals(2, this.texture.prefetchedFrames.size());
        assertEquals(2, this.texture.preparedFrameCount);
    }

    @Test
    void retainsFramesAcrossIntroToMainTransition() {
        this.texture.introFrameCount = 2;
        this.texture.playbackIntro = true;
        this.texture.playbackIndex = 0;
        this.texture.decodeIntro = true;
        this.texture.decodeIndex = 1;

        this.iterate(1000L);
        this.iterate(11001L);

        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        assertEquals(2, pending.size());
        assertTrue(pending.get(0).intro);
        assertEquals(1, pending.get(0).index);
        assertFalse(pending.get(1).intro);
        assertEquals(0, pending.get(1).index);
        assertFalse(this.texture.decodeIntro);
        assertEquals(1, this.texture.decodeIndex);
    }

    @Test
    void retainsLastFrameAndNextLoopsFirstFrame() {
        this.texture.frameCount = 4;
        this.texture.playbackIndex = 2;
        this.texture.decodeIndex = 3;

        this.iterate(1000L);
        this.iterate(11001L);

        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        assertEquals(2, pending.size());
        assertEquals(3, pending.get(0).index);
        assertEquals(0, pending.get(1).index);
        assertEquals(1, this.texture.decodeIndex);
        assertEquals(0, this.texture.cycles.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resetReleasesRetainedFramesAndRestartsTheCorrectSequence(boolean hasIntro) {
        this.texture.introFrameCount = hasIntro ? 2 : 0;
        this.iterate(1000L);
        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        this.iterate(11001L);
        int staleGeneration = this.texture.streamGeneration.get();

        this.texture.requestPlaybackReset();

        this.assertReleased(pending);
        assertEquals(0, this.texture.decodeIndex);
        assertEquals(hasIntro, this.texture.decodeIntro);
        assertFalse(this.texture.playbackInitialized);
        this.texture.lastResourceLocationCall = 12000L;
        this.texture.streamIteration(staleGeneration, 12000L);
        assertTrue(this.texture.prefetchedFrames.isEmpty());
        this.iterate(12000L);
        assertEquals(0, this.texture.prefetchedFrames.getFirst().index);
        assertEquals(hasIntro, this.texture.prefetchedFrames.getFirst().intro);
    }

    @Test
    void closeReleasesRetainedFramesAndStopsFurtherDecoding() {
        this.iterate(1000L);
        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        this.iterate(11001L);

        this.texture.close();

        this.assertReleased(pending);
        assertTrue(this.texture.isClosed());
        this.texture.lastResourceLocationCall = 12000L;
        this.iterate(12000L);
        assertEquals(2, this.texture.preparedFrameCount);
    }

    @Test
    void streamingFailureReleasesRetainedFrames() {
        this.iterate(1000L);
        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        this.iterate(11001L);

        this.texture.failStreaming("Expected test failure", null);

        this.assertReleased(pending);
        assertTrue(this.texture.isLoadingFailed());
        assertFalse(this.texture.isPlaying());
    }

    @Test
    void terminalSingleMainFrameStillReleasesUnneededPrefetch() {
        this.texture.frameCount = 1;
        this.texture.introFrameCount = 2;
        this.texture.playbackIntro = true;
        this.texture.playbackIndex = 1;
        this.texture.decodeIndex = 0;
        this.iterate(1000L);
        List<AfmaTexture.PreparedFrame> pending = List.copyOf(this.texture.prefetchedFrames);
        this.texture.playbackIntro = false;
        this.texture.playbackIndex = 0;

        this.iterate(11001L);

        this.assertReleased(pending);
        assertEquals(2, this.texture.preparedFrameCount);
    }

    @Test
    void pauseRetainsPendingFramesWithoutDecodingMore() {
        this.iterate(1000L);
        AfmaTexture.PreparedFrame first = this.texture.prefetchedFrames.getFirst();
        this.texture.pause();

        this.iterate(11001L);

        assertSame(first, this.texture.prefetchedFrames.getFirst());
        assertNotNull(first.primaryPayload);
        assertEquals(2, this.texture.preparedFrameCount);
    }

    private void iterate(long now) {
        this.texture.streamIteration(this.texture.streamGeneration.get(), now);
    }

    private void assertReleased(List<AfmaTexture.PreparedFrame> frames) {
        assertFalse(frames.isEmpty());
        assertTrue(this.texture.prefetchedFrames.isEmpty());
        for (AfmaTexture.PreparedFrame frame : frames) {
            assertNull(frame.primaryPayload);
            assertNull(frame.patchPayload);
        }
    }

    private static final class InMemoryAfmaTexture extends AfmaTexture {

        private int preparedFrameCount;

        private InMemoryAfmaTexture() {
            this.frameCount = 114;
            this.playbackInitialized = true;
            this.playbackIndex = 20;
            this.decodeIndex = 21;
            this.lastResourceLocationCall = 1000L;
        }

        @Override
        protected PreparedFrame prepareNextFrame(int generation) {
            boolean intro = this.decodeIntro;
            int index = this.decodeIndex;
            if (!this.advanceDecodeCursor(generation)) return null;
            this.preparedFrameCount++;
            // Only payload I/O is replaced; queueing, cursor advancement, inactivity, and cleanup use production code.
            return new PreparedFrame(intro, index, 41L, AfmaFrameDescriptor.full("frame"), new RawPayload(new byte[]{1}), new RawPayload(new byte[]{2}));
        }

    }

}
