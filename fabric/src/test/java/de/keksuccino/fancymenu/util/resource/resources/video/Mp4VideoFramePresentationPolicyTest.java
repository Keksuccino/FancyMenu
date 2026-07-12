package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoFramePresentationPolicyTest {

    @Test
    void keepsPresentedFrameDuringTransientBuffering() {
        assertTrue(Mp4VideoFramePresentationPolicy.shouldPresentFrame(true, true, true, false));
    }

    @Test
    void usesFallbackDuringLoadingBeforeFirstFrameWasPresented() {
        assertFalse(Mp4VideoFramePresentationPolicy.shouldPresentFrame(true, false, true, false));
    }

    @Test
    void hidesFrameWhenPlaybackWasNotRequested() {
        assertFalse(Mp4VideoFramePresentationPolicy.shouldPresentFrame(false, true, false, false));
        assertFalse(Mp4VideoFramePresentationPolicy.shouldPresentFrame(false, true, true, false));
    }

    @Test
    void hidesFrameForTerminalPlayerStatus() {
        assertFalse(Mp4VideoFramePresentationPolicy.shouldPresentFrame(true, true, false, true));
    }

    @Test
    void presentsActiveNonLoadingPlayerStatus() {
        assertTrue(Mp4VideoFramePresentationPolicy.shouldPresentFrame(true, false, false, false));
    }

}
