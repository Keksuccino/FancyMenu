package de.keksuccino.fancymenu.mixin.mixins.common.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrowningEpisodeTrackerTest {

    @Test
    void emitsOnlyTheFirstDamageOfAnEpisode() {
        DrowningEpisodeTracker tracker = new DrowningEpisodeTracker();

        assertTrue(tracker.beginEpisode());
        assertFalse(tracker.beginEpisode());

        tracker.recover();

        assertTrue(tracker.beginEpisode());
    }

    @Test
    void reactivationSilentlyAdoptsAnEpisodeStartedWhileDormant() {
        DrowningEpisodeTracker tracker = new DrowningEpisodeTracker();
        tracker.deactivate();
        assertTrue(tracker.needsPreparation());
        tracker.prepare(true);

        assertFalse(tracker.needsPreparation());
        assertFalse(tracker.beginEpisode());

        tracker.recover();

        assertTrue(tracker.beginEpisode());
    }

    @Test
    void reactivationWhileBreathingAllowsTheNextEpisode() {
        DrowningEpisodeTracker tracker = new DrowningEpisodeTracker();
        tracker.beginEpisode();
        tracker.deactivate();
        tracker.prepare(false);

        assertTrue(tracker.beginEpisode());
    }

    @Test
    void repeatedPreparationDoesNotOverwriteActiveState() {
        DrowningEpisodeTracker tracker = new DrowningEpisodeTracker();
        assertTrue(tracker.beginEpisode());

        tracker.prepare(false);

        assertFalse(tracker.beginEpisode());
    }

}
