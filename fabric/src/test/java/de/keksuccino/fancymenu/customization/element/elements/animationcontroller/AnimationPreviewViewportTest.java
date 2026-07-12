package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationPreviewViewportTest {

    @Test
    void defaultsToSafeIdentityViewport() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(12, viewport.toDisplayX(12));
        assertEquals(-7, viewport.toSourceY(-7));
    }

    @Test
    void mapsParentViewportAcrossFullExpandedManagerGrid() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(960, 540, 1920, 1080);

        assertEquals(960, viewport.toDisplayX(480));
        assertEquals(540, viewport.toDisplayY(270));
        assertEquals(480, viewport.toSourceX(960));
        assertEquals(270, viewport.toSourceY(540));
        assertEquals(200, viewport.toDisplayX(100));
        assertEquals(100, viewport.toSourceY(200));
    }

    @Test
    void scalesAxesIndependentlyAndPreservesNegativeOffsets() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(100, 50, 250, 75);

        assertEquals(50, viewport.toDisplayX(20));
        assertEquals(30, viewport.toDisplayY(20));
        assertEquals(-50, viewport.toDisplayX(-20));
        assertEquals(-30, viewport.toDisplayY(-20));
    }

    @ParameterizedTest
    @ValueSource(ints = {-853, -479, -1, 0, 1, 239, 479, 853})
    void expandedViewportHasLosslessSourceRoundTrip(int sourceValue) {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(853, 479, 1920, 1080);

        assertEquals(sourceValue, viewport.toSourceX(viewport.toDisplayX(sourceValue)));
        assertEquals(sourceValue, viewport.toSourceY(viewport.toDisplayY(sourceValue)));
    }

    @Test
    void resizeRemapCapturesOldDisplayAndAppliesNewDisplayWithoutDrift() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(960, 540, 1920, 1080);
        int sourceX = viewport.toSourceX(1200);
        int sourceY = viewport.toSourceY(700);

        viewport.update(960, 540, 1280, 720);

        assertEquals(600, sourceX);
        assertEquals(350, sourceY);
        assertEquals(800, viewport.toDisplayX(sourceX));
        assertEquals(467, viewport.toDisplayY(sourceY));
    }

    @Test
    void nonPositiveDimensionsAreClampedToOne() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(0, -10, 0, -20);

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(9, viewport.toDisplayX(9));
        assertEquals(-4, viewport.toSourceY(-4));
    }

}
