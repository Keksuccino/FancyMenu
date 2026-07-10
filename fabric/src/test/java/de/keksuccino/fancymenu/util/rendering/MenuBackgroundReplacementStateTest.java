package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBackgroundReplacementStateTest {

    @Test
    void recordsSuccessfulReplacementWithinOnePass() {
        MenuBackgroundReplacementState state = new MenuBackgroundReplacementState();
        int previousState = state.beginRenderPass();

        assertFalse(state.isAttempted());
        assertFalse(state.isRendered());
        state.markRendered();

        assertTrue(state.isAttempted());
        assertTrue(state.isRendered());
        state.endRenderPass(previousState);
        assertFalse(state.isAttempted());
        assertFalse(state.isRendered());
    }

    @Test
    void nestedRenderPassRestoresOuterReplacement() {
        MenuBackgroundReplacementState state = new MenuBackgroundReplacementState();
        int previousOuterState = state.beginRenderPass();
        state.markRendered();

        int previousNestedState = state.beginRenderPass();
        assertFalse(state.isAttempted());
        assertFalse(state.isRendered());
        state.endRenderPass(previousNestedState);

        assertTrue(state.isAttempted());
        assertTrue(state.isRendered());
        state.endRenderPass(previousOuterState);
        assertFalse(state.isAttempted());
        assertFalse(state.isRendered());
    }

    @Test
    void failedAttemptIsRememberedForTheRestOfThePass() {
        MenuBackgroundReplacementState state = new MenuBackgroundReplacementState();
        int previousState = state.beginRenderPass();

        assertTrue(state.beginAttempt());

        assertTrue(state.isAttempted());
        assertFalse(state.isRendered());
        assertFalse(state.beginAttempt());
        state.endRenderPass(previousState);
    }

    @Test
    void wrappedDirtDepthRestoresAcrossNestedCalls() {
        MenuBackgroundReplacementState state = new MenuBackgroundReplacementState();
        int previousOuterDepth = state.beginWrappedDirtCall();
        int previousNestedDepth = state.beginWrappedDirtCall();

        assertTrue(state.isDirtCallWrapped());
        state.endWrappedDirtCall(previousNestedDepth);
        assertTrue(state.isDirtCallWrapped());
        state.endWrappedDirtCall(previousOuterDepth);
        assertFalse(state.isDirtCallWrapped());
    }

}
