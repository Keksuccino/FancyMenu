package de.keksuccino.fancymenu.customization.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutRetirementTrackerTest {

    @Test
    void inactiveReplacementIsDestroyedImmediately() {
        LayoutRetirementTracker tracker = new LayoutRetirementTracker();
        TrackingLayout layout = new TrackingLayout();

        tracker.retire(List.of(layout), List.of());

        assertTrue(layout.destroyed);
        assertEquals(1, layout.destroyCalls);
    }

    @Test
    void activeReplacementStaysWarmUntilNoLayerRetainsIt() {
        LayoutRetirementTracker tracker = new LayoutRetirementTracker();
        TrackingLayout layout = new TrackingLayout();

        tracker.retire(List.of(layout), List.of(layout));
        tracker.releaseNotIn(List.of(layout));
        assertFalse(layout.destroyed);

        tracker.releaseNotIn(List.of());
        assertTrue(layout.destroyed);
        assertEquals(1, layout.destroyCalls);
    }

    @Test
    void destroyAllDeduplicatesLoadedAndRetiredIdentities() {
        LayoutRetirementTracker tracker = new LayoutRetirementTracker();
        TrackingLayout sharedLayout = new TrackingLayout();
        TrackingLayout loadedLayout = new TrackingLayout();
        tracker.retire(List.of(sharedLayout), List.of(sharedLayout));

        tracker.destroyAll(List.of(sharedLayout, loadedLayout));

        assertTrue(sharedLayout.destroyed);
        assertTrue(loadedLayout.destroyed);
        assertEquals(1, sharedLayout.destroyCalls);
        assertEquals(1, loadedLayout.destroyCalls);
    }

    private static final class TrackingLayout extends Layout {

        private boolean destroyed;
        private int destroyCalls;

        private TrackingLayout() {
            super("test-screen");
        }

        @Override
        public void destroy() {
            if (this.destroyed) return;
            this.destroyed = true;
            this.destroyCalls++;
            super.destroy();
        }
    }
}
