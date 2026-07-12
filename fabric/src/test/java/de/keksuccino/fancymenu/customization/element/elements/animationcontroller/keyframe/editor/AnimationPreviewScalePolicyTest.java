package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationPreviewScalePolicyTest {

    @Test
    void usesDefaultScaleWithoutLayoutOverrides() {
        assertEquals(2.0D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, 2.0D, 0.0D, 0, 0));
    }

    @Test
    void forcedFractionalScaleOverridesDefaultScale() {
        assertEquals(1.5D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, 4.0D, 1.5D, 0, 0));
    }

    @Test
    void autoScalingMatchesParentEditorFormula() {
        assertEquals(4.0D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, 2.0D, 0.0D, 960, 540));
        assertEquals(3.0D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, 4.0D, 1.5D, 960, 540));
    }

    @Test
    void invalidForcedScaleFallsBackToOne() {
        assertEquals(1.0D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, 2.0D, -3.0D, 0, 0));
        assertEquals(1.0D, AnimationPreviewScalePolicy.resolveParentGuiScale(1920, 1080, Double.NaN, 0.0D, 0, 0));
    }

    @ParameterizedTest
    @CsvSource({"1920, 2.0, 960", "1921, 2.0, 961", "0, 2.0, 1", "1920, 0.0, 1920"})
    void viewportDimensionsUseFramebufferPixelsAndCeilDivision(int framebufferDimension, double guiScale, int expected) {
        assertEquals(expected, AnimationPreviewScalePolicy.calculateViewportDimension(framebufferDimension, guiScale));
    }

    @Test
    void managerScaleIsCorrectedOnlyWhenItExceedsParentScale() {
        assertTrue(AnimationPreviewScalePolicy.shouldCorrectManagerScale(3.0D, 2.0D));
        assertFalse(AnimationPreviewScalePolicy.shouldCorrectManagerScale(2.0D, 2.0D));
        assertFalse(AnimationPreviewScalePolicy.shouldCorrectManagerScale(1.0D, 2.0D));
    }

}
