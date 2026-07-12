package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationPreviewViewportTest {

    @Test
    void preservesValuesWhenSourceAndDisplayDimensionsMatch() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(854, 480, 854, 480);

        assertEquals(427, viewport.toDisplayX(427));
        assertEquals(240, viewport.toDisplayY(240));
        assertEquals(427, viewport.toSourceX(427));
        assertEquals(240, viewport.toSourceY(240));
    }

    @Test
    void scalesEachAxisIndependently() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(800, 600, 1200, 1800);

        assertEquals(480, viewport.toDisplayX(320));
        assertEquals(600, viewport.toDisplayY(200));
        assertEquals(320, viewport.toSourceX(480));
        assertEquals(200, viewport.toSourceY(600));
    }

    @Test
    void clampsInvalidDimensionsToOne() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(0, -10, 0, -20);

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(7, viewport.toDisplayX(7));
        assertEquals(-9, viewport.toDisplayY(-9));
        assertEquals(11, viewport.toSourceX(11));
        assertEquals(-13, viewport.toSourceY(-13));
    }

    @Test
    void scalesNegativeOffsetsAndViewportBoundaries() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(400, 300, 800, 900);

        assertEquals(-400, viewport.toDisplayX(-200));
        assertEquals(-450, viewport.toDisplayY(-150));
        assertEquals(800, viewport.toDisplayX(400));
        assertEquals(900, viewport.toDisplayY(300));
        assertEquals(-200, viewport.toSourceX(-400));
        assertEquals(-150, viewport.toSourceY(-450));
    }

    @Test
    void roundTripsRepresentativeSourceValuesWhenDisplayIsNotSmaller() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(853, 479, 1280, 720);
        int[] xValues = new int[]{-853, -852, -427, -1, 0, 1, 426, 852, 853};
        int[] yValues = new int[]{-479, -478, -240, -1, 0, 1, 239, 478, 479};

        for (int value : xValues) assertEquals(value, viewport.toSourceX(viewport.toDisplayX(value)));
        for (int value : yValues) assertEquals(value, viewport.toSourceY(viewport.toDisplayY(value)));
    }

    @Test
    void preservesSourceValuesAcrossDisplayViewportResize() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(854, 480, 1280, 720);
        int oldDisplayX = viewport.toDisplayX(641);
        int oldDisplayWidth = viewport.toDisplayX(213);
        int capturedSourceX = viewport.toSourceX(oldDisplayX);
        int capturedSourceWidth = viewport.toSourceX(oldDisplayWidth);

        viewport.update(854, 480, 1708, 960);
        int resizedDisplayX = viewport.toDisplayX(capturedSourceX);
        int resizedDisplayWidth = viewport.toDisplayX(capturedSourceWidth);

        assertEquals(1282, resizedDisplayX);
        assertEquals(426, resizedDisplayWidth);
        assertEquals(641, viewport.toSourceX(resizedDisplayX));
        assertEquals(213, viewport.toSourceX(resizedDisplayWidth));
    }

}
