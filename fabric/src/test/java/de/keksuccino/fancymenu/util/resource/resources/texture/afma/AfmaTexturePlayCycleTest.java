package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaTexturePlayCycleTest {

    @Test
    void infinitelyLoopingAfmaCountsEveryFinishedCycle() {
        AfmaTexture texture = new AfmaTexture();
        texture.numPlays.set(0);

        assertTrue(texture.handleCycleBoundary());
        assertTrue(texture.handleCycleBoundary());

        assertEquals(2, texture.getCompletedPlayCycles());
        assertTrue(texture.pendingStartEvent);
    }

    @Test
    void finitelyLoopingAfmaStopsRestartingAfterItsLastCycle() {
        AfmaTexture texture = new AfmaTexture();
        texture.numPlays.set(1);

        assertFalse(texture.handleCycleBoundary());

        assertEquals(1, texture.getCompletedPlayCycles());
    }

    @Test
    void heldSingleMainFrameAfterIntroCompletesOneCycleAfterItsDelay() {
        AfmaTexture texture = new AfmaTexture();
        texture.decoded.set(true);
        texture.numPlays.set(0);
        texture.introFrameCount = 3;
        texture.frameCount = 1;
        texture.playbackInitialized = true;
        texture.playbackIntro = false;
        texture.playbackIndex = 0;
        texture.playbackFrameDelayMs = 100L;
        assertTrue(texture.shouldIdleOnTerminalSingleMainFrame());

        // The frame just got shown, so its delay is not over yet
        texture.playbackFrameStartMs = System.currentTimeMillis() + 60_000L;
        texture.advancePlaybackIfNeeded();
        assertEquals(0, texture.getCompletedPlayCycles());

        texture.playbackFrameStartMs = 0L;
        texture.advancePlaybackIfNeeded();
        assertEquals(1, texture.getCompletedPlayCycles());

        texture.advancePlaybackIfNeeded();
        assertEquals(1, texture.getCompletedPlayCycles());
        assertTrue(texture.isPlaying());
    }

}
