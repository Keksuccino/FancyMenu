package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationPreviewViewportTest {

    @Test
    void startsWithSafeIdentityDimensions() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(17, viewport.toDisplayX(17));
        assertEquals(-23, viewport.toSourceY(-23));
    }

    @Test
    void preservesValuesForIdentityViewport() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(854, 480, 854, 480);

        assertEquals(0, viewport.toDisplayX(0));
        assertEquals(427, viewport.toDisplayX(427));
        assertEquals(-240, viewport.toDisplayY(-240));
        assertEquals(853, viewport.toSourceX(853));
        assertEquals(479, viewport.toSourceY(479));
    }

    @Test
    void appliesAxisSpecificUpscaling() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(320, 240, 640, 720);

        assertEquals(246, viewport.toDisplayX(123));
        assertEquals(369, viewport.toDisplayY(123));
        assertEquals(123, viewport.toSourceX(246));
        assertEquals(123, viewport.toSourceY(369));
    }

    @Test
    void appliesAxisSpecificDownscaling() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(640, 480, 320, 120);

        assertEquals(160, viewport.toDisplayX(320));
        assertEquals(120, viewport.toDisplayY(480));
        assertEquals(320, viewport.toSourceX(160));
        assertEquals(480, viewport.toSourceY(120));
    }

    @Test
    void scalesNegativeCoordinatesAroundTheSameOrigin() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(400, 200, 600, 500);

        assertEquals(-150, viewport.toDisplayX(-100));
        assertEquals(-250, viewport.toDisplayY(-100));
        assertEquals(-100, viewport.toSourceX(-150));
        assertEquals(-100, viewport.toSourceY(-250));
    }

    @Test
    void usesMathRoundAtPositiveAndNegativeHalfSteps() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(2, 2, 1, 1);

        assertEquals(1, viewport.toDisplayX(1));
        assertEquals(0, viewport.toDisplayX(-1));
        assertEquals(-1, viewport.toDisplayX(-3));
    }

    @Test
    void clampsAllViewportDimensionsToAtLeastOne() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(0, -20, -30, 0);

        assertEquals(1, viewport.getDisplayWidth());
        assertEquals(1, viewport.getDisplayHeight());
        assertEquals(9, viewport.toDisplayX(9));
        assertEquals(-11, viewport.toDisplayY(-11));
        assertEquals(7, viewport.toSourceX(7));
        assertEquals(-13, viewport.toSourceY(-13));
    }

    @Test
    void roundTripsSourceCoordinatesThroughFractionalHighDpiViewport() {
        AnimationPreviewViewport viewport = new AnimationPreviewViewport();
        viewport.update(427, 241, 853, 721);

        for (int x = -427; x <= 427; x++) {
            assertEquals(x, viewport.toSourceX(viewport.toDisplayX(x)), "x=" + x);
        }
        for (int y = -241; y <= 241; y++) {
            assertEquals(y, viewport.toSourceY(viewport.toDisplayY(y)), "y=" + y);
        }
    }

}
