package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeferredPlayerReleaseControllerTest {

    @Test
    void scheduledReleaseClaimsPlayerExactlyOnce() {
        DeferredPlayerReleaseController<Object> controller = new DeferredPlayerReleaseController<>();
        Object player = new Object();
        long token = controller.schedule(player);

        assertTrue(controller.isScheduled(token));
        assertTrue(controller.claimScheduled(player, token));
        assertFalse(controller.isScheduled(token));
        assertFalse(controller.claimScheduled(player, token));
        assertNull(controller.claimForSoundEngineReload());
    }

    @Test
    void soundEngineReloadInvalidatesQueuedRelease() {
        DeferredPlayerReleaseController<Object> controller = new DeferredPlayerReleaseController<>();
        Object player = new Object();
        long token = controller.schedule(player);

        assertSame(player, controller.claimForSoundEngineReload());

        assertFalse(controller.isScheduled(token));
        assertFalse(controller.claimScheduled(player, token));
        assertNull(controller.claimForSoundEngineReload());
    }

    @Test
    void wrongPlayerCannotStealScheduledRelease() {
        DeferredPlayerReleaseController<Object> controller = new DeferredPlayerReleaseController<>();
        Object player = new Object();
        long token = controller.schedule(player);

        assertFalse(controller.claimScheduled(new Object(), token));
        assertSame(player, controller.claimForSoundEngineReload());
    }
}
