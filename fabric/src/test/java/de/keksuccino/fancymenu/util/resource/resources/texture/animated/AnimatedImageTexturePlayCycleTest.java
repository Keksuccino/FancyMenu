package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks play cycles, pausing and restarting of animated textures, on a fake clock that steps like a game rendering at 100 FPS.
 */
class AnimatedImageTexturePlayCycleTest {

    private static final long STEP_NANOS = 10_000_000L;

    private long now = 1_000_000_000L;
    private final AnimatedImageTestSupport.ManualExecutor executor = new AnimatedImageTestSupport.ManualExecutor(() -> this.now);
    private final List<AnimatedImageTexture> textures = new ArrayList<>();

    @AfterEach
    void closeTextures() {
        for (AnimatedImageTexture texture : this.textures) TestTextures.closeAndCheckMemory(texture, this.executor);
    }

    @Test
    void infinitelyLoopingAnimationCountsEveryFinishedCycle() {
        AnimatedImageTexture texture = this.create(TestTextures.Format.APNG);
        texture.numPlays.set(AnimatedImage.INFINITE_PLAYS);

        assertTrue(texture.handleCycleBoundary(1));
        assertTrue(texture.handleCycleBoundary(1));

        assertEquals(2, texture.getCompletedPlayCycles());
        assertTrue(texture.pendingStartEvent);
        assertTrue(texture.isPlaying());
    }

    @Test
    void finitelyLoopingAnimationStopsRestartingAfterItsLastCycle() {
        AnimatedImageTexture texture = this.create(TestTextures.Format.APNG);
        texture.numPlays.set(2);

        assertTrue(texture.handleCycleBoundary(1));
        assertFalse(texture.handleCycleBoundary(1));

        assertEquals(2, texture.getCompletedPlayCycles());
    }

    @Test
    void stoppingFinishedAnimationMakesItPlayableAgain() {
        AnimatedImageTexture texture = this.create(TestTextures.Format.APNG);
        texture.numPlays.set(1);
        assertFalse(texture.handleCycleBoundary(1));
        assertTrue(texture.maxLoopsReached);
        assertFalse(texture.isPlaying());

        texture.stop();

        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
        assertTrue(texture.restartRequested);
    }

    @Test
    void pausingAndPlayingKeepsTheProgress() {
        AnimatedImageTexture texture = this.create(TestTextures.Format.APNG);
        texture.numPlays.set(AnimatedImage.INFINITE_PLAYS);
        texture.handleCycleBoundary(1);

        texture.pause();
        texture.pause();
        assertTrue(texture.isPaused());
        assertFalse(texture.isPlaying());

        texture.play();
        assertFalse(texture.isPaused());
        assertTrue(texture.isPlaying());
        assertEquals(1, texture.getCompletedPlayCycles());
        assertFalse(texture.restartRequested);
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void stillImageCountsAsPlayedOnceItGetsShown(TestTextures.Format format) {
        AnimatedImageTexture texture = this.load(format, format.stillImage());
        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());

        this.render(texture);

        // Things that wait for a texture to finish playing, like game intros, must not wait forever for still images
        assertFalse(texture.isPlaying());
        assertEquals(1, texture.getCompletedPlayCycles());
        texture.reset();
        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
        this.render(texture);
        assertFalse(texture.isPlaying());
        assertEquals(1, texture.getCompletedPlayCycles());
        assertEquals(1, TestTextures.uploads(texture));
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void pauseWhileRenderedHoldsTheFrameForItsRemainingTime(TestTextures.Format format) {
        AnimatedImageTexture texture = this.load(format, format.fourFrames());
        // The first frame shows for 100 ms, it got paused after 50 ms of them and stays paused for 500 ms
        this.renderFor(texture, 50_000_000L);
        texture.pause();
        this.renderFor(texture, 500_000_000L);
        assertEquals(1, TestTextures.uploads(texture));
        texture.play();

        this.renderFor(texture, 40_000_000L);
        assertEquals(1, TestTextures.uploads(texture));
        this.renderFor(texture, 20_000_000L);
        assertEquals(2, TestTextures.uploads(texture));
    }

    @ParameterizedTest
    @EnumSource(TestTextures.Format.class)
    void pausesOfOtherUsersWithoutRenderingDontStallATexture(TestTextures.Format format) {
        AnimatedImageTexture texture = this.load(format, format.fourFrames());
        // Every frame, one user shows the texture and another one pauses it afterwards, like widgets pause the backgrounds of their hidden states
        for (long elapsed = 0L; elapsed <= 1_000_000_000L; elapsed += STEP_NANOS) {
            texture.play();
            this.render(texture);
            texture.pause();
            this.now += STEP_NANOS;
        }

        // Frames of 100 ms in 1 second, the first frame and ten frames after it
        assertEquals(11, TestTextures.uploads(texture));
        assertEquals(2, texture.getCompletedPlayCycles());
    }

    @Test
    void listenerCanRestartTheTextureWhenItsLastPlayFinished() {
        AnimatedImageTexture texture = this.load(TestTextures.Format.APNG, ApngTestFiles.threeFrames(1));
        List<Boolean> finished = new ArrayList<>();
        TestTextures.recorder(texture).onFinishedPlaying = () -> {
            finished.add(texture.isPlaying());
            if (finished.size() == 1) texture.reset();
        };

        // Two plays of three frames of 100 ms each
        this.renderFor(texture, 1_000_000_000L);

        assertEquals(2, finished.size());
        assertFalse(texture.isPlaying());
        assertEquals(6, TestTextures.uploads(texture));
    }

    @Test
    void playsSkippedWhileNobodyRenderedTheTextureCountAsFinished() {
        // Two frames of 20 ms, five plays. The stream decodes a few frames ahead, which get dropped while nobody renders the texture for a while.
        byte[] file = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(2, 5)
                .frameControl(4, 4, 0, 0, 1, 50, 0, 0).imageData(ApngTestFiles.solid(4, 4, 255, 0, 0, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 0, 255, 0, 255))
                .end();
        AnimatedImageTexture texture = this.load(TestTextures.Format.APNG, file);
        this.renderFor(texture, 30_000_000L);
        this.now += AnimatedFrameStream.INACTIVITY_TIMEOUT_NANOS + STEP_NANOS;
        this.executor.runTasks();

        this.renderFor(texture, 1_000_000_000L);

        assertEquals(5, texture.getCompletedPlayCycles());
        assertFalse(texture.isPlaying());
    }

    @Test
    void framesThatALaterFrameOfTheSameRenderReplacesDontGetUploaded() throws IOException {
        // Six frames of 20 ms that each cover the whole canvas, rendered every 50 ms, so two or three frames are due per render, of which only the last one needs an upload
        byte[] file = ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header().animationControl(6, 0)
                .frameControl(4, 4, 0, 0, 1, 50, 0, 0).imageData(ApngTestFiles.solid(4, 4, 255, 0, 0, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 0, 255, 0, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 0, 0, 255, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 255, 255, 0, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 0, 255, 255, 255))
                .frame(4, 4, 0, 0, 1, 50, 0, 0, ApngTestFiles.solid(4, 4, 255, 0, 255, 255))
                .end();
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedImageTexture texture = this.load(TestTextures.Format.APNG, file);
        this.render(texture);

        for (int render = 0; render < 10; render++) {
            this.now += 50_000_000L;
            this.render(texture);
            TestTextures.UploadRecorder recorder = TestTextures.recorder(texture);
            assertArrayEquals(expected[recorder.lastUploadedFrame], recorder.pixels, "render " + render);
        }

        assertEquals(11, TestTextures.uploads(texture));
    }

    private void renderFor(@NotNull AnimatedImageTexture texture, long durationNanos) {
        for (long elapsed = 0L; elapsed < durationNanos; elapsed += STEP_NANOS) {
            this.render(texture);
            this.now += STEP_NANOS;
        }
    }

    private void render(@NotNull AnimatedImageTexture texture) {
        this.executor.runTasks();
        assertNotNull(texture.getResourceLocation());
    }

    @NotNull
    private AnimatedImageTexture create(@NotNull TestTextures.Format format) {
        AnimatedImageTexture texture = format.createTexture(this.executor, () -> this.now);
        this.textures.add(texture);
        return texture;
    }

    @NotNull
    private AnimatedImageTexture load(@NotNull TestTextures.Format format, @NotNull byte[] file) {
        AnimatedImageTexture texture = this.create(format);
        texture.populate(() -> new AnimatedImageTestSupport.MemorySource(file), "image");
        this.executor.runTasks();
        return texture;
    }

}
