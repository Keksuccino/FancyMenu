package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AnimationPreviewViewportTest {

    @Test
    void identityViewportPreservesPositionsAndSizes() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(1920, 1080, 1920, 1080);

        assertEquals(1920, viewport.getDisplayWidth());
        assertEquals(1080, viewport.getDisplayHeight());
        assertEquals(731, viewport.toDisplayX(731));
        assertEquals(-219, viewport.toDisplayY(-219));
        assertEquals(731, viewport.toSourceX(731));
        assertEquals(-219, viewport.toSourceY(-219));
    }

    @Test
    void asymmetricViewportScalesEachAxisIndependently() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(1600, 900, 2400, 1800);

        assertEquals(1200, viewport.toDisplayX(800));
        assertEquals(900, viewport.toDisplayY(450));
        assertEquals(800, viewport.toSourceX(1200));
        assertEquals(450, viewport.toSourceY(900));
    }

    @Test
    void conversionUsesMathRoundForPositiveAndNegativeValues() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(3, 3, 2, 2);

        assertEquals(1, viewport.toDisplayX(1));
        assertEquals(-1, viewport.toDisplayX(-1));
        assertEquals(2, viewport.toSourceY(1));
        assertEquals(-1, viewport.toSourceY(-1));
    }

    @Test
    void invalidDimensionsAreClampedBeforeConversion() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(0, -10, 0, -20);

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(17, viewport.toDisplayX(17));
        assertEquals(-23, viewport.toSourceY(-23));
    }

    @Test
    void sourceRoundTripIsLosslessWhenDisplayViewportIsNotSmaller() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(7, 11, 10, 19);

        for (int value = -100; value <= 100; value++) {
            assertEquals(value, viewport.toSourceX(viewport.toDisplayX(value)));
            assertEquals(value, viewport.toSourceY(viewport.toDisplayY(value)));
        }
    }

    @Test
    void smallerDisplayViewportCanCollapseSourceCoordinates() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(8, 8, 4, 4);

        assertEquals(viewport.toDisplayX(1), viewport.toDisplayX(2));
        assertNotEquals(1, viewport.toSourceX(viewport.toDisplayX(1)));
    }

}
