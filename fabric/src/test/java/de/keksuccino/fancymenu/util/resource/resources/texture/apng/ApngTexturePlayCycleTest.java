package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngTexturePlayCycleTest {

    @Test
    void infinitelyLoopingApngCountsEveryFinishedCycle() {
        ApngTexture texture = new ApngTexture();
        texture.numPlays.set(ApngImage.INFINITE_PLAYS);

        assertTrue(texture.handleCycleBoundary());
        assertTrue(texture.handleCycleBoundary());

        assertEquals(2, texture.getCompletedPlayCycles());
        assertTrue(texture.pendingStartEvent);
        assertTrue(texture.isPlaying());
    }

    @Test
    void finitelyLoopingApngStopsRestartingAfterItsLastCycle() {
        ApngTexture texture = new ApngTexture();
        texture.numPlays.set(2);

        assertTrue(texture.handleCycleBoundary());
        assertFalse(texture.handleCycleBoundary());

        assertEquals(2, texture.getCompletedPlayCycles());
    }

    @Test
    void stoppingFinishedApngMakesItPlayableAgain() {
        ApngTexture texture = new ApngTexture();
        texture.numPlays.set(1);
        assertFalse(texture.handleCycleBoundary());
        // Playback marks the max loops as reached when no restart follows
        texture.maxLoopsReached = true;
        assertFalse(texture.isPlaying());

        texture.stop();

        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
        assertTrue(texture.restartRequested);
    }

    @Test
    void pausingAndPlayingKeepsTheProgress() {
        ApngTexture texture = new ApngTexture();
        texture.numPlays.set(ApngImage.INFINITE_PLAYS);
        texture.handleCycleBoundary();

        texture.pause();
        texture.pause();
        assertTrue(texture.isPaused());
        assertFalse(texture.isPlaying());
        assertTrue(texture.pausedAtNanos >= 0L);

        texture.play();
        assertFalse(texture.isPaused());
        assertTrue(texture.isPlaying());
        assertEquals(1, texture.getCompletedPlayCycles());
        assertFalse(texture.restartRequested);
    }

    @Test
    void invalidImageDataPublishesTheFailure() {
        ApngTexture texture = new ApngTexture();

        ApngTexture.populateTexture(texture, new ByteArrayInputStream(new byte[0]), "invalid.png");

        assertTrue(texture.loadingFailed.get());
        assertTrue(texture.isReady());
        assertTrue(texture.isLoadingFailed());
        assertFalse(texture.isLoadingCompleted());
    }

    @Test
    void closedTextureDoesNotLoadAnymore() {
        ApngTexture texture = new ApngTexture();
        texture.closed.set(true);

        ApngTexture.populateTexture(texture, new ByteArrayInputStream(new byte[0]), "invalid.png");

        assertFalse(texture.loadingFailed.get());
        assertFalse(texture.decoded.get());
    }

}
