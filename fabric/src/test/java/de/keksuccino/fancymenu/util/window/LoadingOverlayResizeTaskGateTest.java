package de.keksuccino.fancymenu.util.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingOverlayResizeTaskGateTest {

    private static final long FRAME_NANOS = 16_000_000L;

    private long nowNanos = 1_000_000_000L;
    private final LoadingOverlayResizeTaskGate gate = new LoadingOverlayResizeTaskGate(() -> this.nowNanos);

    @Test
    void fullscreenSwitchHoldsTasksUntilFrameWithNewSize() {
        this.gate.onFramePresenting(1280, 768);
        this.gate.onFullscreenSwitch(true, 1280, 768);
        this.gate.onDisplayResized(true, 1920, 1080);

        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1920, 1080);
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void fullscreenSwitchKeepsHoldingTasksUntilLateSizeCallback() {
        this.gate.onFramePresenting(1280, 768);
        this.gate.onFullscreenSwitch(true, 1280, 768);
        // The switch resizes the display itself, before the new framebuffer size arrived
        this.gate.onDisplayResized(true, 1280, 768);

        for (int i = 0; i < 5; i++) {
            assertFalse(this.nextFrameRunsTasks(true));
            this.gate.onFramePresenting(1280, 768);
        }
        this.gate.onDisplayResized(true, 1920, 1080);
        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1920, 1080);
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void fullscreenSwitchBeforeFirstFrameHoldsTasksUntilFrameWithNewSize() {
        this.gate.onFullscreenSwitch(true, 1280, 768);

        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1280, 768);
        this.gate.onDisplayResized(true, 1920, 1080);
        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1920, 1080);
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void switchWithoutSizeChangeReleasesOnTimeout() {
        this.gate.onFramePresenting(1920, 1080);
        this.gate.onFullscreenSwitch(true, 1920, 1080);
        this.gate.onDisplayResized(true, 1920, 1080);

        assertFalse(this.nextFrameRunsTasks(true));
        long firstHeldBackAt = this.nowNanos;
        this.gate.onFramePresenting(1920, 1080);
        this.nowNanos = firstHeldBackAt + LoadingOverlayResizeTaskGate.TIMEOUT_NANOS - 1L;
        assertFalse(this.gate.shouldRunTasks(true));
        this.nowNanos = firstHeldBackAt + LoadingOverlayResizeTaskGate.TIMEOUT_NANOS;
        assertTrue(this.gate.shouldRunTasks(true));
    }

    @Test
    void timeoutOnlyCountsTimeTheTasksAreHeldBack() {
        this.gate.onFramePresenting(1280, 768);
        this.gate.onDisplayResized(true, 1920, 1080);
        // Resizing the current screen takes longer than the timeout
        this.nowNanos += 2L * LoadingOverlayResizeTaskGate.TIMEOUT_NANOS;

        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1920, 1080);
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void timeoutKeepsGateOpenUntilLoadingOverlayEnds() {
        this.gate.onFramePresenting(1920, 1080);
        this.gate.onFullscreenSwitch(true, 1920, 1080);
        assertFalse(this.nextFrameRunsTasks(true));
        this.nowNanos += LoadingOverlayResizeTaskGate.TIMEOUT_NANOS;
        assertTrue(this.gate.shouldRunTasks(true));

        this.gate.onFullscreenSwitch(true, 1920, 1080);
        assertTrue(this.nextFrameRunsTasks(true));
        this.gate.onDisplayResized(true, 1280, 768);
        assertTrue(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1280, 768);

        assertTrue(this.nextFrameRunsTasks(false));
        this.gate.onFullscreenSwitch(true, 1280, 768);
        assertFalse(this.nextFrameRunsTasks(true));
    }

    @Test
    void resizeDuringLoadingOverlayHoldsTasksUntilFrameWithNewSize() {
        this.gate.onFramePresenting(854, 480);
        this.gate.onDisplayResized(true, 1200, 700);

        assertFalse(this.nextFrameRunsTasks(true));
        this.gate.onFramePresenting(1200, 700);
        assertTrue(this.nextFrameRunsTasks(true));
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void resizeKeepingFramebufferSizeDoesNotHoldTasks() {
        // For example a GUI scale change
        this.gate.onFramePresenting(854, 480);
        this.gate.onDisplayResized(true, 854, 480);

        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void resizeBeforeFirstFrameDoesNotHoldTasks() {
        this.gate.onDisplayResized(true, 1200, 700);

        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void resizeEveryFrameStillRunsTasksEveryOtherFrame() {
        int width = 854;
        this.gate.onFramePresenting(width, 480);
        this.gate.onDisplayResized(true, ++width, 480);

        for (int i = 0; i < 10; i++) {
            assertFalse(this.nextFrameRunsTasks(true));
            // Presenting the frame polls the resize of the next frame
            this.gate.onFramePresenting(width, 480);
            this.gate.onDisplayResized(true, ++width, 480);
            assertTrue(this.nextFrameRunsTasks(true));
            this.gate.onFramePresenting(width, 480);
            this.gate.onDisplayResized(true, ++width, 480);
        }
    }

    @Test
    void neverHoldsTasksWithoutLoadingOverlay() {
        this.gate.onFramePresenting(1280, 768);
        this.gate.onFullscreenSwitch(false, 1280, 768);
        assertTrue(this.nextFrameRunsTasks(false));
        this.gate.onFramePresenting(1280, 768);

        this.gate.onDisplayResized(false, 1920, 1080);
        assertTrue(this.nextFrameRunsTasks(false));
        this.gate.onFramePresenting(1920, 1080);

        // Switching or resizing outside the loading overlay must not close the gate for a loading overlay that starts later
        assertTrue(this.nextFrameRunsTasks(true));
    }

    @Test
    void loadingOverlayEndingOpensClosedGate() {
        this.gate.onFramePresenting(1280, 768);
        this.gate.onFullscreenSwitch(true, 1280, 768);
        assertFalse(this.nextFrameRunsTasks(true));

        assertTrue(this.nextFrameRunsTasks(false));
        assertTrue(this.nextFrameRunsTasks(true));
    }

    /**
     * Advances the clock by one frame and asks the gate like the game does right before running its queued tasks.
     */
    private boolean nextFrameRunsTasks(boolean loadingOverlayActive) {
        this.nowNanos += FRAME_NANOS;
        return this.gate.shouldRunTasks(loadingOverlayActive);
    }

}
