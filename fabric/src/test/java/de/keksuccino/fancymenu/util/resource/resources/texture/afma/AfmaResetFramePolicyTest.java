package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfmaResetFramePolicyTest {

    @Test
    void skipsOnlyTheSelectedFrameZeroAfterSnapshotRestore() {
        assertTrue(AfmaResetFramePolicy.shouldSkipFrameApplication(true, true, 0, 2));
        assertTrue(AfmaResetFramePolicy.shouldSkipFrameApplication(true, false, 0, 0));

        assertFalse(AfmaResetFramePolicy.shouldSkipFrameApplication(true, false, 0, 2));
        assertFalse(AfmaResetFramePolicy.shouldSkipFrameApplication(true, true, 1, 2));
        assertFalse(AfmaResetFramePolicy.shouldSkipFrameApplication(true, false, 1, 0));
    }

    @Test
    void appliesFrameZeroNormallyWhenSnapshotRestoreWasCancelled() {
        assertFalse(AfmaResetFramePolicy.shouldSkipFrameApplication(false, true, 0, 2));
        assertFalse(AfmaResetFramePolicy.shouldSkipFrameApplication(false, false, 0, 0));
    }

}
