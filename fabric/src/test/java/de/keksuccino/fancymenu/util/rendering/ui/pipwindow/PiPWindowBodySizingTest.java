package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiPWindowBodySizingTest {

    /** UI scales divided by GUI scales, like 1.5 / 4, and scales that don't divide evenly. */
    private static final double[] RENDER_SCALES = {0.25, 0.3125, 1.0 / 3.0, 0.375, 0.5, 0.5625, 0.6, 0.625, 2.0 / 3.0, 0.75, 0.8, 1.0, 1.125, 1.25, 1.5, 2.0, 2.5, 3.0};

    @Test
    void screenFillsItsBody() {
        // 73 GUI pixels at 0.375 GUI pixels per unit are 194.67 units, which the screen rounds up
        assertEquals(195, PiPWindowBodySizing.screenSize(73, 0.375));
        assertEquals(196, PiPWindowBodySizing.screenSize(98, 0.5));
        assertEquals(98, PiPWindowBodySizing.screenSize(98, 1.0));
        assertEquals(49, PiPWindowBodySizing.screenSize(98, 2.0));
        assertEquals(1, PiPWindowBodySizing.screenSize(0, 0.5));
    }

    @Test
    void bodyIsTheSmallestOneWhoseScreenIsBigEnough() {
        for (double renderScale : RENDER_SCALES) {
            for (int screenSize = 1; screenSize <= 700; screenSize++) {
                int bodySize = PiPWindowBodySizing.bodySizeForScreenSize(screenSize, renderScale);
                int size = screenSize;

                assertTrue(bodySize >= 1);
                assertTrue(PiPWindowBodySizing.screenSize(bodySize, renderScale) >= screenSize, () -> "body " + bodySize + " is too small for a screen of " + size + " at " + renderScale);
                if (bodySize > 1) {
                    assertTrue(PiPWindowBodySizing.screenSize(bodySize - 1, renderScale) < screenSize, () -> "body " + bodySize + " is bigger than a screen of " + size + " needs at " + renderScale);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {188, 197, 211, 233, 317})
    void screenOfAWelcomeWindowBodyIsNeverSmallerThanTheHeightItsTextNeeds(int neededHeight) {
        // The frame takes whole GUI pixels, however small the render scale makes a unit of the screen
        for (double renderScale : RENDER_SCALES) {
            int frame = (Math.max(1, (int) Math.round(renderScale)) * 2) + Math.max(1, (int) Math.round(21 * renderScale));
            int windowSize = PiPWindowBodySizing.bodySizeForScreenSize(neededHeight, renderScale) + frame;

            assertTrue(PiPWindowBodySizing.screenSize(windowSize - frame, renderScale) >= neededHeight);
            // A screen is at most the units of one GUI pixel bigger than it has to be
            assertTrue(PiPWindowBodySizing.screenSize(windowSize - frame, renderScale) < neededHeight + (1.0 / renderScale) + 1.0);
        }
    }

    @Test
    void bodyOfAScreenAtScaleOneHasTheSizeOfThatScreen() {
        for (int screenSize = 1; screenSize <= 500; screenSize++) {
            assertEquals(screenSize, PiPWindowBodySizing.bodySizeForScreenSize(screenSize, 1.0));
        }
    }

}
