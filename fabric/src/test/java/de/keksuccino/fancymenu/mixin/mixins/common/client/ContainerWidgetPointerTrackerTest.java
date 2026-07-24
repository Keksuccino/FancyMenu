package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.mixin.support.client.ContainerWidgetPointerTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ContainerWidgetPointerTrackerTest {

    @Test
    void capturedOwnerRemainsAvailableUntilRelease() {
        ContainerWidgetPointerTracker<Object> tracker = new ContainerWidgetPointerTracker<>();
        Object owner = new Object();

        tracker.capture(0, owner);

        assertSame(owner, tracker.get(0));
        assertSame(owner, tracker.release(0));
        assertNull(tracker.get(0));
        assertNull(tracker.release(0));
    }

    @Test
    void buttonsKeepIndependentOwners() {
        ContainerWidgetPointerTracker<String> tracker = new ContainerWidgetPointerTracker<>();

        tracker.capture(0, "left");
        tracker.capture(1, "right");

        assertEquals("left", tracker.release(0));
        assertEquals("right", tracker.get(1));
        assertEquals("right", tracker.release(1));
    }

    @Test
    void captureReplacesThePreviousOwnerForTheSameButton() {
        ContainerWidgetPointerTracker<String> tracker = new ContainerWidgetPointerTracker<>();

        tracker.capture(0, "old");
        tracker.capture(0, "new");

        assertEquals("new", tracker.get(0));
    }

    @Test
    void clearingAButtonDropsStaleOwnershipWithoutAffectingOtherButtons() {
        ContainerWidgetPointerTracker<String> tracker = new ContainerWidgetPointerTracker<>();

        tracker.capture(0, "stale");
        tracker.capture(1, "right");
        tracker.clear(0);

        assertNull(tracker.get(0));
        assertEquals("right", tracker.get(1));
    }

}
