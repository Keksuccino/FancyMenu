package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelOverrideTextureChangeDetectorTest {

    @Test
    void animatedFrameLocationIsNotPartOfTheBakeState() {
        Object animatedTexture = new Object();

        boolean rebuild = ModelOverrideTextureChangeDetector.requiresModelRebuild(animatedTexture, animatedTexture, 64, 64, 32, 32, true, true, false, false);

        assertFalse(rebuild);
        assertTrue(ModelOverrideTextureChangeDetector.hasRenderLocationChanged("fancymenu:dynamic/gif_frame_1", "fancymenu:dynamic/gif_frame_0"));
    }

    @Test
    void resourceDimensionsReadinessAndFailureStateRemainBakeInputs() {
        Object firstResource = new Object();
        Object replacementResource = new Object();

        assertTrue(ModelOverrideTextureChangeDetector.requiresModelRebuild(replacementResource, firstResource, 64, 64, 32, 32, true, true, false, false));
        assertTrue(ModelOverrideTextureChangeDetector.requiresModelRebuild(firstResource, firstResource, 128, 64, 32, 32, true, true, false, false));
        assertTrue(ModelOverrideTextureChangeDetector.requiresModelRebuild(firstResource, firstResource, 64, 64, 64, 32, true, true, false, false));
        assertTrue(ModelOverrideTextureChangeDetector.requiresModelRebuild(firstResource, firstResource, 64, 64, 32, 32, false, true, false, false));
        assertTrue(ModelOverrideTextureChangeDetector.requiresModelRebuild(firstResource, firstResource, 64, 64, 32, 32, true, true, true, false));
    }

}
