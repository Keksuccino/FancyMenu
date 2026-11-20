package de.keksuccino.fancymenu.customization.decorationoverlay;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorationOverlayFrameTrackerTest {

    private final AtomicLong frame = new AtomicLong(10L);
    private final AtomicLong nanos = new AtomicLong(1_000_000_000L);
    private final DecorationOverlayFrameTracker tracker = new DecorationOverlayFrameTracker(this.frame::get, this.nanos::get);

    @Test
    void bothPassesOfAFrameShareOneUpdate() {
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.BACKGROUND));
        assertTrue(this.tracker.isUpdatePending());
        assertEquals(0.0F, this.tracker.consumeUpdate());
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.FOREGROUND));
        assertFalse(this.tracker.isUpdatePending());
        assertEquals(1L, this.tracker.getUpdateCount());

        this.nextFrame(16_000_000L);
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.BACKGROUND));
        assertTrue(this.tracker.isUpdatePending());
        assertEquals(0.016F, this.tracker.consumeUpdate(), 0.00001F);
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.FOREGROUND));
        assertFalse(this.tracker.isUpdatePending());
        assertEquals(2L, this.tracker.getUpdateCount());
    }

    @Test
    void aMissingBackgroundPassStillUpdatesOncePerFrame() {
        for (int i = 0; i < 5; i++) {
            assertTrue(this.tracker.beginPass(DecorationOverlayPass.FOREGROUND));
            assertTrue(this.tracker.isUpdatePending());
            this.tracker.consumeUpdate();
            this.nextFrame(16_000_000L);
        }
        assertEquals(5L, this.tracker.getUpdateCount());
        assertEquals(0L, this.tracker.getPassCount(DecorationOverlayPass.BACKGROUND));
        assertEquals(5L, this.tracker.getPassCount(DecorationOverlayPass.FOREGROUND));
    }

    @Test
    void aPassRepeatedInTheSameFrameGetsSkipped() {
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.FOREGROUND));
        this.tracker.consumeUpdate();
        assertFalse(this.tracker.beginPass(DecorationOverlayPass.FOREGROUND));
        assertFalse(this.tracker.isUpdatePending());
        // The other pass still runs once in the same frame
        assertTrue(this.tracker.beginPass(DecorationOverlayPass.BACKGROUND));
        assertFalse(this.tracker.beginPass(DecorationOverlayPass.BACKGROUND));
        assertEquals(1L, this.tracker.getPassCount(DecorationOverlayPass.FOREGROUND));
    }

    @Test
    void longGapsAdvanceTheSimulationByTheLimitAtMost() {
        this.tracker.beginPass(DecorationOverlayPass.FOREGROUND);
        this.tracker.consumeUpdate();
        this.nextFrame(5_000_000_000L);
        this.tracker.beginPass(DecorationOverlayPass.FOREGROUND);
        assertEquals(DecorationOverlayFrameTracker.MAX_DELTA_SECONDS, this.tracker.consumeUpdate());
        // A clock that goes backwards never gives a negative step
        this.nextFrame(-1_000_000L);
        this.tracker.beginPass(DecorationOverlayPass.FOREGROUND);
        assertEquals(0.0F, this.tracker.consumeUpdate());
    }

    private void nextFrame(long elapsedNanos) {
        this.frame.incrementAndGet();
        this.nanos.addAndGet(elapsedNanos);
    }

}
