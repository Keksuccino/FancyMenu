package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which first frame after a reset already shows through the restored reset frame snapshot, so it doesn't get applied onto the composed snapshot again.
 */
class AfmaTextureResetFrameTest {

    @Test
    void restoredSnapshotIsIntroFrameZeroOfTexturesWithIntro() {
        AfmaTexture texture = texture(true, 2);

        assertTrue(texture.isRestoredResetFrame(frame(true, 0)));
        assertFalse(texture.isRestoredResetFrame(frame(true, 1)));
        assertFalse(texture.isRestoredResetFrame(frame(false, 0)));
    }

    @Test
    void restoredSnapshotIsMainFrameZeroOfTexturesWithoutIntro() {
        AfmaTexture texture = texture(true, 0);

        assertTrue(texture.isRestoredResetFrame(frame(false, 0)));
        assertFalse(texture.isRestoredResetFrame(frame(false, 1)));
    }

    @Test
    void framesGetAppliedNormallyWhenNoSnapshotGotRestored() {
        assertFalse(texture(false, 2).isRestoredResetFrame(frame(true, 0)));
        assertFalse(texture(false, 0).isRestoredResetFrame(frame(false, 0)));
    }

    private static AfmaTexture texture(boolean restored, int introFrameCount) {
        AfmaTexture texture = new AfmaTexture();
        texture.playbackResetFrameRestored = restored;
        texture.introFrameCount = introFrameCount;
        return texture;
    }

    private static AfmaTexture.PreparedFrame frame(boolean intro, int index) {
        return new AfmaTexture.PreparedFrame(intro, index, 100L, AfmaFrameDescriptor.full("frames/" + index + ".bin"), null, null);
    }

}
