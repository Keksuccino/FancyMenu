package de.keksuccino.fancymenu.util.watermedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaUtilTest {

    @Test
    void renderingAvailabilityDependsOnlyOnWatermediaPresence() {
        assertAll(
                () -> assertTrue(WatermediaUtil.isWatermediaRenderingAvailable(true)),
                () -> assertFalse(WatermediaUtil.isWatermediaRenderingAvailable(false)));
    }

    @Test
    void videoPlaybackRequiresWatermediaAndItsBinaries() {
        assertAll(
                () -> assertTrue(WatermediaUtil.isWatermediaVideoPlaybackAvailable(true, true)),
                () -> assertFalse(WatermediaUtil.isWatermediaVideoPlaybackAvailable(true, false)),
                () -> assertFalse(WatermediaUtil.isWatermediaVideoPlaybackAvailable(false, true)),
                () -> assertFalse(WatermediaUtil.isWatermediaVideoPlaybackAvailable(false, false)));
    }

}
