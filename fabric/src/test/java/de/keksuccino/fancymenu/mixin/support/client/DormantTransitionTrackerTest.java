package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DormantTransitionTrackerTest {

    @Test
    void firstObservationIsDistinguishedFromLaterChanges() {
        DormantTransitionTracker<String> tracker = new DormantTransitionTracker<>();

        DormantTransitionTracker.Phase initial = tracker.observe(true, "initial");
        DormantTransitionTracker.Phase unchanged = tracker.observe(true, "initial");
        DormantTransitionTracker.Phase changed = tracker.observe(true, "changed");

        assertEquals(DormantTransitionTracker.Phase.INITIAL, initial);
        assertEquals(DormantTransitionTracker.Phase.UNCHANGED, unchanged);
        assertEquals(DormantTransitionTracker.Phase.CHANGED, changed);
        assertEquals("initial", tracker.previousValue());
        assertEquals("changed", tracker.currentValue());
    }

    @Test
    void disabledTrackingClearsValuesAndReactivationIsSilent() {
        DormantTransitionTracker<String> tracker = new DormantTransitionTracker<>();
        tracker.observe(true, "before");

        DormantTransitionTracker.Phase disabled = tracker.observe(false, "ignored");
        assertEquals(DormantTransitionTracker.Phase.DISABLED, disabled);
        assertNull(tracker.currentValue());

        DormantTransitionTracker.Phase reactivated = tracker.observe(true, "after");
        assertEquals(DormantTransitionTracker.Phase.REACTIVATED, reactivated);
        assertNull(tracker.previousValue());
        assertEquals("after", tracker.currentValue());

        DormantTransitionTracker.Phase changed = tracker.observe(true, "later");
        assertEquals(DormantTransitionTracker.Phase.CHANGED, changed);
        assertEquals("after", tracker.previousValue());
    }

    @Test
    void nullIsAValidTrackedBaseline() {
        DormantTransitionTracker<String> tracker = new DormantTransitionTracker<>();

        assertEquals(DormantTransitionTracker.Phase.INITIAL, tracker.observe(true, null));
        assertEquals(DormantTransitionTracker.Phase.UNCHANGED, tracker.observe(true, null));
        assertEquals(DormantTransitionTracker.Phase.CHANGED, tracker.observe(true, "value"));
    }

    @Test
    void explicitDormancyResetSuppressesTheNextBaseline() {
        DormantTransitionTracker<Boolean> tracker = new DormantTransitionTracker<>();
        tracker.observe(true, false);

        tracker.resetForDormancy();

        assertEquals(DormantTransitionTracker.Phase.REACTIVATED, tracker.observe(true, true));
    }

    @Test
    void baselineResetAllowsARealSourceRestartToEmitAnInitialState() {
        DormantTransitionTracker<Boolean> tracker = new DormantTransitionTracker<>();
        tracker.observe(true, true);

        tracker.resetBaseline();

        assertEquals(DormantTransitionTracker.Phase.INITIAL, tracker.observe(true, true));
    }
}
