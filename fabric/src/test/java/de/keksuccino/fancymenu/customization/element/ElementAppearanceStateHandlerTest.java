package de.keksuccino.fancymenu.customization.element;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ElementAppearanceStateHandlerTest {

    @AfterEach
    void clearState() {
        ElementAppearanceStateHandler.clear();
    }

    @Test
    void sameMenuRebuildResumesInterruptedFadeIn() {
        ElementAppearanceStateHandler.ElementState firstInstance = state("screen-a", "button");
        assertFadeOpacity(0.02F, firstInstance.beginOrResumeFadeIn(true, 0.02F));
        firstInstance.updateFadeIn(0.35F);

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-a");

        ElementAppearanceStateHandler.ElementState rebuiltInstance = state("screen-a", "button");
        assertFadeOpacity(0.35F, rebuiltInstance.beginOrResumeFadeIn(true, 0.02F));
    }

    @Test
    void interruptedFirstTimeFadeInFinishesOnRealMenuSwitch() {
        ElementAppearanceStateHandler.ElementState firstVisit = state("screen-a", "button");
        firstVisit.beginOrResumeFadeIn(true, 0.02F);
        firstVisit.updateFadeIn(0.35F);

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-b");

        assertNull(state("screen-a", "button").beginOrResumeFadeIn(true, 0.02F));
    }

    @Test
    void everyTimeFadeInRestartsAfterRealMenuSwitch() {
        ElementAppearanceStateHandler.ElementState firstVisit = state("screen-a", "button");
        firstVisit.beginOrResumeFadeIn(false, 0.02F);
        firstVisit.updateFadeIn(0.35F);

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-b");

        assertFadeOpacity(0.02F, state("screen-a", "button").beginOrResumeFadeIn(false, 0.02F));
    }

    @Test
    void firstTimeFadeThatNeverStartedStillRunsOnLaterVisit() {
        state("screen-a", "button");

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-b");

        assertFadeOpacity(0.02F, state("screen-a", "button").beginOrResumeFadeIn(true, 0.02F));
    }

    @Test
    void everyTimeFadeCanReplayForASecondVisibilityCycleInTheSameMenu() {
        ElementAppearanceStateHandler.ElementState state = state("screen-a", "button");
        state.beginOrResumeFadeIn(false, 0.02F);
        state.finishFadeIn();

        state.restartEveryTimeFadeIn();

        assertFadeOpacity(0.02F, state.beginOrResumeFadeIn(false, 0.02F));
    }

    @Test
    void sameMenuRebuildKeepsAppearanceDelayDeadline() {
        ElementAppearanceStateHandler.ElementState firstInstance = state("screen-a", "button");
        assertEquals(2000L, firstInstance.beginOrResumeAppearanceDelay(true, 2000L, 1000L));

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-a");

        assertEquals(2000L, state("screen-a", "button").beginOrResumeAppearanceDelay(true, 2500L, 1500L));
    }

    @Test
    void interruptedFirstTimeAppearanceDelayFinishesOnRealMenuSwitch() {
        state("screen-a", "button").beginOrResumeAppearanceDelay(true, 2000L, 1000L);

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-b");

        assertEquals(-1L, state("screen-a", "button").beginOrResumeAppearanceDelay(true, 4000L, 3000L));
    }

    @Test
    void everyTimeAppearanceDelayRestartsAfterRealMenuSwitch() {
        state("screen-a", "button").beginOrResumeAppearanceDelay(false, 2000L, 1000L);

        ElementAppearanceStateHandler.onScreenChanged("screen-a", "screen-b");

        assertEquals(4000L, state("screen-a", "button").beginOrResumeAppearanceDelay(false, 4000L, 3000L));
    }

    @Test
    void everyTimeAppearanceDelayCanReplayForASecondVisibilityCycleInTheSameMenu() {
        ElementAppearanceStateHandler.ElementState state = state("screen-a", "button");
        state.beginOrResumeAppearanceDelay(false, 2000L, 1000L);
        state.finishAppearanceDelay();

        state.restartEveryTimeAppearanceDelay();

        assertEquals(4000L, state.beginOrResumeAppearanceDelay(false, 4000L, 3000L));
    }

    @Test
    void elapsedAppearanceDelayIsCompletedDuringARebuild() {
        ElementAppearanceStateHandler.ElementState state = state("screen-a", "button");
        state.beginOrResumeAppearanceDelay(true, 2000L, 1000L);

        assertEquals(-1L, state.beginOrResumeAppearanceDelay(true, 4000L, 2000L));
        assertEquals(-1L, state.beginOrResumeAppearanceDelay(true, 5000L, 3000L));
    }

    @Test
    void appearanceStateIsScopedByScreenAndElementIdentifier() {
        state("custom-gui-a", "button").beginOrResumeFadeIn(true, 0.02F);
        state("custom-gui-a", "button").updateFadeIn(0.35F);

        ElementAppearanceStateHandler.onScreenChanged("custom-gui-a", "custom-gui-b");

        assertFadeOpacity(0.02F, state("custom-gui-b", "button").beginOrResumeFadeIn(true, 0.02F));
        assertNull(state("custom-gui-a", "button").beginOrResumeFadeIn(true, 0.02F));
        assertFadeOpacity(0.02F, state("custom-gui-a", "other-button").beginOrResumeFadeIn(true, 0.02F));
    }

    private static void assertFadeOpacity(float expected, Float actual) {
        assertNotNull(actual);
        assertEquals(expected, actual, 0.0001F);
    }

    private static ElementAppearanceStateHandler.ElementState state(String screenIdentifier, String elementIdentifier) {
        return ElementAppearanceStateHandler.getState(screenIdentifier, elementIdentifier);
    }

}
