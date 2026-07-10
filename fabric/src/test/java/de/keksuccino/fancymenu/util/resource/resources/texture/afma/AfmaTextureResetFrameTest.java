package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaTextureResetFrameTest {

    @Test
    void usesRestoredIntroFrameZero() {
        assertTrue(AfmaTexture.shouldUseRestoredFrame(true, 0, true, 3));
    }

    @Test
    void usesRestoredNormalFrameZeroWhenThereIsNoIntro() {
        assertTrue(AfmaTexture.shouldUseRestoredFrame(true, 0, false, 0));
    }

    @Test
    void rejectsFramesThatAreNotTheExpectedRestoredFrameZero() {
        assertFalse(AfmaTexture.shouldUseRestoredFrame(false, 0, true, 3));
        assertFalse(AfmaTexture.shouldUseRestoredFrame(true, 1, true, 3));
        assertFalse(AfmaTexture.shouldUseRestoredFrame(true, 0, false, 3));
    }

}
