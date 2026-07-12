package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSoundSubscriptionPolicyTest {

    @Test
    void subscribesOnlyOnTheFirstActivationAndUnsubscribesOnTheLastDeactivation() {
        WorldSoundSubscriptionPolicy policy = new WorldSoundSubscriptionPolicy();

        assertEquals(WorldSoundSubscriptionPolicy.Decision.ADD, policy.setActive(true));
        assertTrue(policy.isRegistered());
        assertEquals(WorldSoundSubscriptionPolicy.Decision.NONE, policy.setActive(true));
        assertEquals(WorldSoundSubscriptionPolicy.Decision.REMOVE, policy.setActive(false));
        assertFalse(policy.isRegistered());
        assertEquals(WorldSoundSubscriptionPolicy.Decision.NONE, policy.setActive(false));
    }

    @Test
    void defersRemovalDuringDispatchAndQueuesItOnlyOnce() {
        WorldSoundSubscriptionPolicy policy = new WorldSoundSubscriptionPolicy();
        policy.setActive(true);
        policy.beginDispatch();

        assertEquals(WorldSoundSubscriptionPolicy.Decision.DEFER_REMOVE, policy.setActive(false));
        assertTrue(policy.isRegistered());
        assertTrue(policy.isRemovalQueued());
        assertEquals(WorldSoundSubscriptionPolicy.Decision.NONE, policy.setActive(false));
        policy.endDispatch();
        assertEquals(WorldSoundSubscriptionPolicy.Decision.REMOVE, policy.reevaluateDeferredRemoval(false));
        assertFalse(policy.isRegistered());
        assertFalse(policy.isRemovalQueued());
    }

    @Test
    void deferredRemovalRechecksCurrentActivity() {
        WorldSoundSubscriptionPolicy policy = new WorldSoundSubscriptionPolicy();
        policy.setActive(true);
        policy.beginDispatch();
        policy.setActive(false);
        policy.endDispatch();

        assertEquals(WorldSoundSubscriptionPolicy.Decision.NONE, policy.setActive(true));
        assertEquals(WorldSoundSubscriptionPolicy.Decision.NONE, policy.reevaluateDeferredRemoval(true));
        assertTrue(policy.isRegistered());
        assertFalse(policy.isRemovalQueued());
    }

    @Test
    void nestedDispatchesRemainProtectedUntilTheOutermostCallbackReturns() {
        WorldSoundSubscriptionPolicy policy = new WorldSoundSubscriptionPolicy();
        policy.setActive(true);
        policy.beginDispatch();
        policy.beginDispatch();
        policy.endDispatch();

        assertEquals(WorldSoundSubscriptionPolicy.Decision.DEFER_REMOVE, policy.setActive(false));
        policy.endDispatch();
        assertEquals(WorldSoundSubscriptionPolicy.Decision.REMOVE, policy.reevaluateDeferredRemoval(false));
    }

    @Test
    void rejectsDispatchDepthUnderflow() {
        WorldSoundSubscriptionPolicy policy = new WorldSoundSubscriptionPolicy();

        assertThrows(IllegalStateException.class, policy::endDispatch);
    }

}
