package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.util.resource.resources.texture.gif.GifTestFiles;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays GIF images through the real GIF texture, on a fake clock that steps like a game rendering at 100 FPS, and checks the GIF specific playback rules.
 */
class GifTexturePlaybackTest {

    private static final long STEP_NANOS = 10_000_000L;

    private long now = 1_000_000_000L;
    private final AnimatedImageTestSupport.ManualExecutor executor = new AnimatedImageTestSupport.ManualExecutor(() -> this.now);
    private AnimatedImageTexture texture;

    @AfterEach
    void closeTexture() {
        if (this.texture != null) TestTextures.closeAndCheckMemory(this.texture, this.executor);
    }

    @Test
    void loopCountPlaysOneMoreTimeThanItSaysLikeBrowsers() {
        this.load(threeFrames(1));

        this.playUntilFinished(200);

        assertEquals(2, this.texture.getCompletedPlayCycles());
        assertFalse(this.texture.isPlaying());
        assertEquals(6, TestTextures.uploads(this.texture));
    }

    @Test
    void gifWithoutLoopExtensionPlaysOnce() {
        this.load(threeFrames(-1));

        this.playUntilFinished(200);

        assertEquals(1, this.texture.getCompletedPlayCycles());
        assertFalse(this.texture.isPlaying());
        assertEquals(3, TestTextures.uploads(this.texture));
    }

    @Test
    void loopCountZeroLoopsForever() {
        this.load(threeFrames(0));

        this.playUntilFinished(400);

        // A play takes 60 ms, and the last of the 400 renders happens 3990 ms after the first one, which showed the first frame of the first play
        assertEquals(66, this.texture.getCompletedPlayCycles());
        assertTrue(this.texture.isPlaying());
    }

    @Test
    void framesWithoutVisiblePixelsOnlyTakeUpTheirTime() {
        // The second frame lies completely outside the canvas, so it shows the canvas of the first frame for its 100 ms without any upload
        GifTestFiles.Gif gif = GifTestFiles.gif(4, 4, TestTextures.GIF_COLORS).netscapeLoop(0);
        gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(4, 4, 0));
        gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 1).at(10, 10));
        gif.graphicControl(1, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 2));
        this.load(gif.end());

        // Three plays of 300 ms, then the first frame of the fourth play
        this.playUntilFinished(91);

        assertEquals(3, this.texture.getCompletedPlayCycles());
        assertEquals(7, TestTextures.uploads(this.texture));
    }

    /**
     * Renders the texture every 10 ms of the fake clock, until the animation is over or the renders ran out.
     */
    private void playUntilFinished(int maxRenders) {
        for (int render = 0; (render < maxRenders) && !this.texture.maxLoopsReached; render++) {
            this.executor.runTasks();
            assertNotNull(this.texture.getResourceLocation());
            this.now += STEP_NANOS;
        }
    }

    private void load(@NotNull byte[] file) {
        this.texture = TestTextures.Format.GIF.createTexture(this.executor, () -> this.now);
        this.texture.populate(() -> new AnimatedImageTestSupport.MemorySource(file), "test.gif");
        this.executor.runTasks();
    }

    /**
     * Three 4x4 frames of 20 ms with the given NETSCAPE loop count, or without loop extension if the count is negative.
     */
    @NotNull
    private static byte[] threeFrames(int loopCount) {
        GifTestFiles.Gif gif = GifTestFiles.gif(4, 4, TestTextures.GIF_COLORS);
        if (loopCount >= 0) gif.netscapeLoop(loopCount);
        gif.graphicControl(1, 2, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(4, 4, 0));
        gif.graphicControl(1, 2, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 1));
        gif.graphicControl(1, 2, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(2, 2, 2).at(2, 2));
        return gif.end();
    }

}
