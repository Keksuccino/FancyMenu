package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslInputDeltaStateTest {

    @Test
    void firstCaptureBaselinesPersistentTrackerValuesWithoutReplayingThem() {
        GlslInputDeltaState state = new GlslInputDeltaState();

        GlslInputDeltaState.Delta delta = state.capture(14.5D, -8.25D, 3.0D, -2.0D, 41, true);

        assertZero(delta);
    }

    @Test
    void subsequentCaptureEmitsOnlyScrollAccumulatedSinceThePreviousFrame() {
        GlslInputDeltaState state = new GlslInputDeltaState();
        state.capture(10.0D, 20.0D, 0.0D, 0.0D, 7, true);

        GlslInputDeltaState.Delta firstDelta = state.capture(12.5D, 17.0D, 0.0D, 0.0D, 7, true);
        GlslInputDeltaState.Delta secondDelta = state.capture(12.5D, 17.0D, 0.0D, 0.0D, 7, true);

        assertAll(() -> assertEquals(2.5D, firstDelta.scrollX()), () -> assertEquals(-3.0D, firstDelta.scrollY()), () -> assertEquals(0.0D, firstDelta.mouseX()), () -> assertEquals(0.0D, firstDelta.mouseY()), () -> assertZero(secondDelta));
    }

    @Test
    void mouseDeltaIsEmittedOnlyWhenTheGlobalMoveCounterAdvances() {
        GlslInputDeltaState state = new GlslInputDeltaState();
        state.capture(0.0D, 0.0D, 4.0D, 5.0D, 12, true);

        GlslInputDeltaState.Delta stale = state.capture(0.0D, 0.0D, 9.0D, -7.0D, 12, true);
        GlslInputDeltaState.Delta moved = state.capture(0.0D, 0.0D, 9.0D, -7.0D, 13, true);

        assertAll(() -> assertZero(stale), () -> assertEquals(9.0D, moved.mouseX()), () -> assertEquals(-7.0D, moved.mouseY()), () -> assertEquals(0.0D, moved.scrollX()), () -> assertEquals(0.0D, moved.scrollY()));
    }

    @Test
    void disabledCapturesKeepAdvancingBaselinesAndFirstEnabledFrameStaysZero() {
        GlslInputDeltaState state = new GlslInputDeltaState();
        state.capture(5.0D, 6.0D, 1.0D, 2.0D, 20, true);

        GlslInputDeltaState.Delta disabled = state.capture(15.0D, 16.0D, 7.0D, 8.0D, 21, false);
        GlslInputDeltaState.Delta enabled = state.capture(15.0D, 16.0D, 7.0D, 8.0D, 21, true);
        GlslInputDeltaState.Delta next = state.capture(16.0D, 14.0D, -3.0D, 4.0D, 22, true);

        assertAll(() -> assertZero(disabled), () -> assertZero(enabled), () -> assertEquals(1.0D, next.scrollX()), () -> assertEquals(-2.0D, next.scrollY()), () -> assertEquals(-3.0D, next.mouseX()), () -> assertEquals(4.0D, next.mouseY()));
    }

    @Test
    void resetForcesTheNextCaptureToEstablishFreshBaselines() {
        GlslInputDeltaState state = new GlslInputDeltaState();
        state.capture(1.0D, 2.0D, 0.0D, 0.0D, 1, true);
        state.capture(3.0D, 4.0D, 5.0D, 6.0D, 2, true);

        state.reset();
        GlslInputDeltaState.Delta afterReset = state.capture(30.0D, 40.0D, 50.0D, 60.0D, 20, true);

        assertZero(afterReset);
    }

    private static void assertZero(GlslInputDeltaState.Delta delta) {
        assertAll(() -> assertEquals(0.0D, delta.scrollX()), () -> assertEquals(0.0D, delta.scrollY()), () -> assertEquals(0.0D, delta.mouseX()), () -> assertEquals(0.0D, delta.mouseY()));
    }
}
