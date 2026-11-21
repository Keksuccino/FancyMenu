package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks pausing of FMA textures through their real stream thread iterations and renders, on a fake clock that steps like a game rendering at 100 FPS.
 */
class FmaTexturePauseTest {

    private static final long STEP_MS = 10L;
    /** Most stream thread iterations that can run without waiting, which are a few per shown frame. **/
    private static final int MAX_ITERATIONS_WITHOUT_WAITING = 8;

    private long now = 1_000_000L;
    private final HeadlessFmaTexture texture = new HeadlessFmaTexture();

    @AfterEach
    void closeTexture() {
        this.texture.close();
    }

    @Test
    void pauseWhileRenderedHoldsTheFrameForItsRemainingTime() {
        // The first frame shows for 100 ms, it got paused after 50 ms of them and stays paused for 500 ms
        this.renderFor(50L);
        assertEquals(1, this.texture.uploads);
        this.texture.pause();
        this.renderFor(500L);
        assertEquals(1, this.texture.uploads);
        this.texture.play();

        this.renderFor(40L);
        assertEquals(1, this.texture.uploads);
        this.renderFor(20L);
        assertEquals(2, this.texture.uploads);
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

        // Frames of 100 ms in 1 second, the first frame and ten frames after it
        assertEquals(11, this.texture.uploads);
        assertEquals(2, this.texture.getCompletedPlayCycles());
    }

    private void renderFor(long durationMs) {
        for (long elapsed = 0L; elapsed < durationMs; elapsed += STEP_MS) {
            this.runStream();
            this.texture.getResourceLocation();
            this.now += STEP_MS;
        }
    }

    /**
     * Runs stream thread iterations until the stream thread waits, like it does between two frames of the game.
     */
    private void runStream() {
        for (int i = 0; i < MAX_ITERATIONS_WITHOUT_WAITING; i++) {
            if (this.texture.streamIteration(this.texture.streamGeneration.get(), this.now) > 0L) return;
        }
        fail("The stream thread never waited");
    }

    /**
     * Four frames of 100 ms that loop forever, which decode to tiny images and only get counted instead of uploaded to a texture.
     * Playback runs on the fake clock and the test runs the iterations of the stream thread itself.
     */
    private final class HeadlessFmaTexture extends FmaTexture {

        private int uploads;

        private HeadlessFmaTexture() {
            this.decoded.set(true);
            this.frameCount = 4;
            this.frameDelaysMs = new long[]{100L, 100L, 100L, 100L};
        }

        @Override
        protected long currentTimeMillis() {
            return FmaTexturePauseTest.this.now;
        }

        @Override
        protected void startTickerIfNeeded() {
        }

        @Override
        @NotNull
        protected NativeImage decodeFrameImage(boolean intro, int index) {
            return new NativeImage(1, 1, true);
        }

        @Override
        protected void uploadPendingFrameToTexture() {
            DecodedFrame frame = this.pendingUploadFrame.getAndSet(null);
            if (frame == null) return;
            this.uploads++;
            frame.close();
        }

    }

}
