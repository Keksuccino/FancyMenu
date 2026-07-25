package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslPassFrameStateTest {

    @Test
    void startsAtFrameZeroAndAdvancesOnlyWhenCommitted() {
        GlslPassFrameState state = new GlslPassFrameState();

        state.activate("shader-a", true);
        int first = state.currentFrame();
        state.commitFrame();
        int second = state.currentFrame();

        assertAll(() -> assertEquals(0, first), () -> assertEquals(1, second), () -> assertEquals(1L, state.committedFrameCount()));
    }

    @Test
    void keepsHistoryForTheSameIdentityAndExistingStorage() {
        GlslPassFrameState state = activeState("shader-a", 3);

        assertFalse(state.historyIdentityChanged("shader-a"));
        state.activate("shader-a", false);

        assertAll(() -> assertEquals(3, state.currentFrame()), () -> assertEquals(3L, state.committedFrameCount()));
    }

    @Test
    void resetsOnlyAfterAHistoryIdentityOrStorageChange() {
        GlslPassFrameState sourceChanged = activeState("shader-a", 4);
        GlslPassFrameState resized = activeState("shader-a", 5);

        assertTrue(sourceChanged.historyIdentityChanged("shader-b"));
        sourceChanged.activate("shader-b", false);
        resized.activate("shader-a", true);

        assertAll(() -> assertEquals(0, sourceChanged.currentFrame()), () -> assertEquals(0, resized.currentFrame()));
    }

    @Test
    void deactivationMakesReactivationAFrameZeroLifecycle() {
        GlslPassFrameState state = activeState("shader-a", 2);

        state.deactivate();
        boolean changed = state.historyIdentityChanged("shader-a");
        state.activate("shader-a", true);

        assertAll(() -> assertTrue(changed), () -> assertEquals(0, state.currentFrame()), () -> assertEquals(0L, state.committedFrameCount()));
    }

    @Test
    void imageHistoryUsesOnlyAcceptedProgramAndRoutingIdentities() {
        GlslPassFrameState state = activeState("accepted-image-a|routing-a", 3);

        // A rejected candidate never activates a history identity, so returning to the accepted program preserves iFrame.
        state.activate("accepted-image-a|routing-a", false);
        int afterRejectedCandidate = state.currentFrame();
        state.activate("accepted-image-b|routing-a", false);
        int afterAcceptedProgramChange = state.currentFrame();
        state.commitFrame();
        state.activate("accepted-image-b|routing-b", false);
        int afterRoutingChange = state.currentFrame();

        assertAll(() -> assertEquals(3, afterRejectedCandidate), () -> assertEquals(0, afterAcceptedProgramChange), () -> assertEquals(0, afterRoutingChange));
    }

    private static GlslPassFrameState activeState(String identity, int committedFrames) {
        GlslPassFrameState state = new GlslPassFrameState();
        state.activate(identity, true);
        for (int frame = 0; frame < committedFrames; frame++) {
            state.commitFrame();
        }
        return state;
    }
}
