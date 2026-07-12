package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaFramePresentationPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void loadingStatesStayHiddenBeforeFirstFrame(String statusName) {
        assertFalse(shouldPresentFrame(true, false, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void loadingStatesPreservePreviouslyPresentedFrame(String statusName) {
        assertTrue(shouldPresentFrame(true, true, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOPPED", "ENDED", "ERROR"})
    void terminalStatesStayHiddenAfterFrameWasPresented(String statusName) {
        assertFalse(shouldPresentFrame(true, true, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLAYING", "PAUSED"})
    void activeStatesRemainVisible(String statusName) {
        assertTrue(shouldPresentFrame(true, false, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "BUFFERING", "PLAYING", "PAUSED", "STOPPED"})
    void playRequestIsRequired(String statusName) {
        assertFalse(shouldPresentFrame(false, true, statusName));
    }

    private static boolean shouldPresentFrame(boolean playRequested, boolean framePresented, String statusName) {
        boolean loadingPlayerStatus = WatermediaFramePresentationPolicy.isLoadingPlayerStatus(statusName);
        boolean terminalPlayerStatus = WatermediaFramePresentationPolicy.isTerminalPlayerStatus(statusName);
        return WatermediaFramePresentationPolicy.shouldPresentFrame(playRequested, framePresented, loadingPlayerStatus, terminalPlayerStatus);
    }

}
