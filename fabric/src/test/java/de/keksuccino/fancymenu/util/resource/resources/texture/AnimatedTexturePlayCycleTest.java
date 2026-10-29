package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedTexturePlayCycleTest {

    @Test
    void infinitelyLoopingGifCountsEveryFinishedCycle() {
        GifTexture texture = new GifTexture();
        texture.numPlays.set(0);

        assertTrue(texture.handleCycleBoundary());
        assertTrue(texture.handleCycleBoundary());

        assertEquals(2, texture.getCompletedPlayCycles());
        assertTrue(texture.pendingStartEvent);
        assertTrue(texture.isPlaying());
    }

    @Test
    void finitelyLoopingGifStopsRestartingAfterItsLastCycle() {
        GifTexture texture = new GifTexture();
        texture.numPlays.set(2);

        assertTrue(texture.handleCycleBoundary());
        assertFalse(texture.handleCycleBoundary());

        assertEquals(2, texture.getCompletedPlayCycles());
    }

    @Test
    void stoppingFinishedGifMakesItPlayableAgain() {
        GifTexture texture = new GifTexture();
        texture.numPlays.set(1);
        assertFalse(texture.handleCycleBoundary());
        // The frame ticker marks the max loops as reached when no restart follows
        texture.maxLoopsReached = true;
        assertFalse(texture.isPlaying());

        texture.stop();

        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
    }

    @Test
    void infinitelyLoopingApngCountsEveryFinishedCycle() {
        ApngTexture texture = new ApngTexture();
        texture.numPlays.set(-1);

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
        // The frame ticker marks the max loops as reached when no restart follows
        texture.maxLoopsReached = true;
        assertFalse(texture.isPlaying());

        texture.stop();

        assertTrue(texture.isPlaying());
        assertEquals(0, texture.getCompletedPlayCycles());
    }

}
