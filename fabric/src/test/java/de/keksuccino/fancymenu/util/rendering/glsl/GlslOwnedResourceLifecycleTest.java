package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslOwnedResourceLifecycleTest {

    @Test
    void acceptsOnlyStrictlyPositiveRenderAreas() {
        assertTrue(GlslOwnedResourceLifecycle.hasRenderableArea(1, 1));
        assertTrue(GlslOwnedResourceLifecycle.hasRenderableArea(1920, 1080));
        assertFalse(GlslOwnedResourceLifecycle.hasRenderableArea(0, 1));
        assertFalse(GlslOwnedResourceLifecycle.hasRenderableArea(1, 0));
        assertFalse(GlslOwnedResourceLifecycle.hasRenderableArea(-1, 1));
        assertFalse(GlslOwnedResourceLifecycle.hasRenderableArea(1, -1));
    }

    @Test
    void releasesOnlyAfterTheFinalExtractedVisibilityOrFadeFrame() {
        GlslOwnedResourceLifecycle lifecycle = new GlslOwnedResourceLifecycle();

        assertFalse(lifecycle.completeExtractionCycle(true, true));
        assertFalse(lifecycle.completeExtractionCycle(true, true));
        assertTrue(lifecycle.completeExtractionCycle(false, true));
        assertFalse(lifecycle.completeExtractionCycle(false, true));
    }

    @Test
    void outerSkipReleasesOnlyOnceAfterPriorExtraction() {
        GlslOwnedResourceLifecycle lifecycle = new GlslOwnedResourceLifecycle();

        assertFalse(lifecycle.completeExtractionCycle(false, true));
        assertFalse(lifecycle.completeExtractionCycle(true, true));
        assertTrue(lifecycle.completeExtractionCycle(false, true));
        assertFalse(lifecycle.completeExtractionCycle(false, true));
    }

    @Test
    void unsettledDelayBoundaryRetainsResourcesForAFollowingFade() {
        GlslOwnedResourceLifecycle lifecycle = new GlslOwnedResourceLifecycle();

        assertFalse(lifecycle.completeExtractionCycle(true, true));
        assertFalse(lifecycle.completeExtractionCycle(false, false));
        assertFalse(lifecycle.completeExtractionCycle(true, true));
        assertTrue(lifecycle.completeExtractionCycle(false, true));
    }

    @Test
    void explicitReleaseClearsPendingOwnership() {
        GlslOwnedResourceLifecycle lifecycle = new GlslOwnedResourceLifecycle();

        assertFalse(lifecycle.completeExtractionCycle(true, true));
        lifecycle.markResourcesReleased();

        assertFalse(lifecycle.completeExtractionCycle(false, true));
    }
}
