package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FmaTexturePlayCycleTest {

    @Test
    void infinitelyLoopingFmaCountsEveryFinishedCycle() {
        FmaTexture texture = new FmaTexture();
        texture.numPlays.set(0);

        assertEquals(FmaTexture.CycleBoundaryResult.RESTART, texture.handleCycleBoundary(texture.streamGeneration.get()));
        assertEquals(FmaTexture.CycleBoundaryResult.RESTART, texture.handleCycleBoundary(texture.streamGeneration.get()));

        assertEquals(2, texture.getCompletedPlayCycles());
        assertTrue(texture.pendingStartEvent);
    }

    @Test
    void finitelyLoopingFmaStopsRestartingAfterItsLastCycle() {
        FmaTexture texture = new FmaTexture();
        texture.numPlays.set(1);

        assertEquals(FmaTexture.CycleBoundaryResult.STOP, texture.handleCycleBoundary(texture.streamGeneration.get()));

        assertEquals(1, texture.getCompletedPlayCycles());
        assertFalse(texture.isPlaying());
    }

}
