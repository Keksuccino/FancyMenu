package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks pausing of AFMA textures through their real playback and stream thread iterations, on a fake clock that steps like a game rendering at 100 FPS.
 */
class AfmaTexturePauseTest {

    private static final long STEP_MS = 10L;

    private long now = 1_000_000L;
    private final HeadlessAfmaTexture texture = new HeadlessAfmaTexture();

    @AfterEach
    void closeTexture() {
        this.texture.close();
    }

    @Test
    void pauseWhileRenderedHoldsTheFrameForItsRemainingTime() {
        // The first frame shows for 100 ms, it got paused after 50 ms of them and stays paused for 500 ms
        this.renderFor(50L);
        this.texture.pause();
        this.renderFor(500L);
        assertEquals(0, this.texture.appliedFrames);
        this.texture.play();

        this.renderFor(40L);
        assertEquals(0, this.texture.appliedFrames);
        this.renderFor(20L);
        assertEquals(1, this.texture.appliedFrames);
    }

    @Test
    void pausesOfOtherUsersWithoutRenderingDontStallATexture() {
        // Every frame, one user shows the texture and another one pauses it afterwards, like widgets pause the backgrounds of their hidden states.
        // The stream thread mostly runs while the texture is paused then.
        for (long elapsed = 0L; elapsed <= 1000L; elapsed += STEP_MS) {
            this.runStream();
            this.texture.play();
            this.texture.getResourceLocation();
            this.texture.pause();
            this.now += STEP_MS;
        }

        // Frames of 100 ms in 1 second, ten frames after the first one
        assertEquals(10, this.texture.appliedFrames);
        assertEquals(2, this.texture.getCompletedPlayCycles());
    }

    private void renderFor(long durationMs) {
        for (long elapsed = 0L; elapsed < durationMs; elapsed += STEP_MS) {
            this.runStream();
            this.texture.getResourceLocation();
            this.now += STEP_MS;
        }
    }

    private void runStream() {
        this.texture.streamIteration(this.texture.streamGeneration.get(), this.now);
    }

    /**
     * Four frames of 100 ms that loop forever, which got the first frame on the canvas when the test started. Frames have no payloads, the canvas only counts the frames
     * that get applied to it, playback runs on the fake clock and the test runs the iterations of the stream thread itself.
     */
    private final class HeadlessAfmaTexture extends AfmaTexture {

        private int appliedFrames;

        private HeadlessAfmaTexture() {
            this.decoded.set(true);
            this.frameCount = 4;
            this.frameDelaysMs = new long[]{100L, 100L, 100L, 100L};
            this.playbackInitialized = true;
            this.playbackIndex = 0;
            this.playbackFrameStartMs = AfmaTexturePauseTest.this.now;
            this.playbackFrameDelayMs = 100L;
            this.decodeIndex = 1;
        }

        @Override
        protected long currentTimeMillis() {
            return AfmaTexturePauseTest.this.now;
        }

        @Override
        protected void startTickerIfNeeded() {
        }

        @Override
        @Nullable
        protected PreparedFrame prepareNextFrame(int generation) {
            boolean intro = this.decodeIntro;
            int index = this.decodeIndex;
            long delay = this.resolveFrameDelay(intro, index);
            if (!this.advanceDecodeCursor(generation)) return null;
            return new PreparedFrame(intro, index, delay, AfmaFrameDescriptor.full("frame"), null, null);
        }

        @Override
        protected void applyPreparedFrame(@NotNull PreparedFrame preparedFrame) {
            this.appliedFrames++;
        }

    }

}
