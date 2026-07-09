package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaFramePresentationPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void loadingStatesStayHiddenBeforeFirstFrame(String statusName) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(statusName, true, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void loadingStatesPreservePreviouslyPresentedFrame(String statusName) {
        assertTrue(WatermediaFramePresentationPolicy.shouldPresentFrame(statusName, true, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ENDED", "ERROR"})
    void terminalStatesStayHiddenAfterFrameWasPresented(String statusName) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(statusName, true, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLAYING", "PAUSED", "UNKNOWN"})
    void activeStatesRemainVisible(String statusName) {
        assertTrue(WatermediaFramePresentationPolicy.shouldPresentFrame(statusName, true, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "BUFFERING", "PLAYING", "PAUSED", "STOPPED"})
    void playRequestIsRequired(String statusName) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(statusName, false, true));
    }

}
