package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaTextureResetFramePolicyTest {

    @Test
    void usesRestoredComposedIntroFrameZero() {
        assertTrue(AfmaTexture.shouldUseRestoredResetFrame(true, true, 0, 2));
    }

    @Test
    void usesRestoredComposedNormalFrameZeroWhenThereIsNoIntro() {
        assertTrue(AfmaTexture.shouldUseRestoredResetFrame(true, false, 0, 0));
    }

    @Test
    void rejectsNonzeroAndSequenceMismatchedFrames() {
        assertFalse(AfmaTexture.shouldUseRestoredResetFrame(true, true, 1, 2));
        assertFalse(AfmaTexture.shouldUseRestoredResetFrame(true, false, 0, 2));
        assertFalse(AfmaTexture.shouldUseRestoredResetFrame(false, true, 0, 2));
    }

}
