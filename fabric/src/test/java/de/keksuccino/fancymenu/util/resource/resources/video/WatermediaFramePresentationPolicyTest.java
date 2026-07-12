package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermediaFramePresentationPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING", "PLAYING", "PAUSED", "UNKNOWN", "STOPPED", "ENDED", "ERROR"})
    void hidesFramesWhenPlaybackWasNotRequested(String statusName) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(false, true, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void hidesInitialLoadingStatesBeforeAFrameWasPresented(String statusName) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(true, false, statusName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "LOADING", "BUFFERING"})
    void preservesThePreviousFrameDuringTransientLoadingStates(String statusName) {
        assertTrue(WatermediaFramePresentationPolicy.shouldPresentFrame(true, true, statusName));
    }

    @ParameterizedTest
    @CsvSource({
            "PLAYING, false",
            "PLAYING, true",
            "PAUSED, false",
            "PAUSED, true",
            "UNKNOWN, false",
            "UNKNOWN, true"
    })
    void presentsNonTerminalStatesRegardlessOfPreviousFrameState(String statusName, boolean framePresented) {
        assertTrue(WatermediaFramePresentationPolicy.shouldPresentFrame(true, framePresented, statusName));
    }

    @ParameterizedTest
    @CsvSource({
            "STOPPED, false",
            "STOPPED, true",
            "ENDED, false",
            "ENDED, true",
            "ERROR, false",
            "ERROR, true"
    })
    void hidesTerminalStatesRegardlessOfPreviousFrameState(String statusName, boolean framePresented) {
        assertFalse(WatermediaFramePresentationPolicy.shouldPresentFrame(true, framePresented, statusName));
    }

}
