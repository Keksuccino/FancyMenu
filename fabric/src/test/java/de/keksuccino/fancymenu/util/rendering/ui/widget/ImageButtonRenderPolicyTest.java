package de.keksuccino.fancymenu.util.rendering.ui.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageButtonRenderPolicyTest {

    @Test
    void baseLabelAlwaysReplacesIcon() {
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(true, false, false, true, true));
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(true, false, true, true, true));
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(true, true, false, true, true));
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(true, true, true, true, true));
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(true, true, true, true, false));
    }

    @Test
    void hoverOnlyLabelRequiresActiveVisibleHoverState() {
        assertTrue(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, true, true, true, true));
        assertFalse(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, true, false, true, true));
        assertFalse(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, true, true, false, true));
        assertFalse(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, true, true, true, false));
    }

    @Test
    void missingLabelsKeepIcon() {
        assertFalse(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, false, false, true, true));
        assertFalse(ImageButtonRenderPolicy.shouldRenderCustomLabel(false, false, true, true, true));
    }

    @Test
    void customLabelSuppressesVanillaIconIndependentlyOfBackground() {
        assertFalse(ImageButtonRenderPolicy.shouldRenderVanillaIcon(true, true));
        assertFalse(ImageButtonRenderPolicy.shouldRenderVanillaIcon(false, true));
    }

    @Test
    void backgroundDecisionRemainsAuthoritativeWithoutLabel() {
        assertTrue(ImageButtonRenderPolicy.shouldRenderVanillaIcon(true, false));
        assertFalse(ImageButtonRenderPolicy.shouldRenderVanillaIcon(false, false));
    }

}
