package de.keksuccino.fancymenu.util.rendering.ui.widget.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ContainerWidgetPointerTrackerTest {

    @Test
    void tracksIndependentOwnersForMultipleButtons() {
        ContainerWidgetPointerTracker<Object> tracker = new ContainerWidgetPointerTracker<>();
        Object leftOwner = new Object();
        Object rightOwner = new Object();

        tracker.claim(0, leftOwner);
        tracker.claim(1, rightOwner);

        assertSame(leftOwner, tracker.owner(0));
        assertSame(rightOwner, tracker.owner(1));
    }

    @Test
    void beginningInteractionClearsOnlyStaleOwnerForSameButton() {
        ContainerWidgetPointerTracker<Object> tracker = new ContainerWidgetPointerTracker<>();
        Object leftOwner = new Object();
        Object rightOwner = new Object();
        tracker.claim(0, leftOwner);
        tracker.claim(1, rightOwner);

        tracker.begin(0);

        assertNull(tracker.owner(0));
        assertSame(rightOwner, tracker.owner(1));
    }

    @Test
    void releaseReturnsAndRemovesOnlyMatchingOwner() {
        ContainerWidgetPointerTracker<Object> tracker = new ContainerWidgetPointerTracker<>();
        Object leftOwner = new Object();
        Object rightOwner = new Object();
        tracker.claim(0, leftOwner);
        tracker.claim(1, rightOwner);

        assertSame(leftOwner, tracker.release(0));
        assertNull(tracker.owner(0));
        assertSame(rightOwner, tracker.owner(1));
    }

    @Test
    void unclaimedInteractionHasNoOwner() {
        ContainerWidgetPointerTracker<Object> tracker = new ContainerWidgetPointerTracker<>();

        tracker.begin(2);

        assertNull(tracker.owner(2));
        assertNull(tracker.release(2));
    }

    @Test
    void trackerInstancesDoNotShareOwnership() {
        ContainerWidgetPointerTracker<Object> first = new ContainerWidgetPointerTracker<>();
        ContainerWidgetPointerTracker<Object> second = new ContainerWidgetPointerTracker<>();
        Object owner = new Object();

        first.claim(0, owner);

        assertSame(owner, first.owner(0));
        assertNull(second.owner(0));
    }

}
