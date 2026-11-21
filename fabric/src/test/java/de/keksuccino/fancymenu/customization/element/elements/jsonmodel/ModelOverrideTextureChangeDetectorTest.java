package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelOverrideTextureChangeDetectorTest {

    @Test
    void renderLocationIsNotPartOfTheBakeState() {
        Object texture = new Object();

        boolean rebuild = ModelOverrideTextureChangeDetector.requiresModelRebuild(texture, texture, 64, 64, 32, 32, true, true, false, false);

        assertFalse(rebuild);
        assertTrue(ModelOverrideTextureChangeDetector.hasRenderLocationChanged("fancymenu:dynamic/gif_texture_2", "fancymenu:dynamic/gif_texture_1"));
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
