package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoSoundEngineLifecyclePolicyTest {

    @Test
    void startupWaitsForMinecraftOpenAlInitialization() {
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldCreatePlayer(true, false, false, false));
    }

    @Test
    void readyOpenAlAllowsAudioBackedPlayerBeforeReloadCompletion() {
        assertTrue(Mp4VideoSoundEngineLifecyclePolicy.shouldCreatePlayer(true, false, true, false));
    }

    @Test
    void failedOpenAlStartupAllowsSilentPlayerAfterReloadCompletion() {
        assertTrue(Mp4VideoSoundEngineLifecyclePolicy.shouldCreatePlayer(true, false, false, true));
    }

    @Test
    void reloadAndMissingPlayIntentBothBlockPlayerCreation() {
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldCreatePlayer(true, true, true, true));
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldCreatePlayer(false, false, true, true));
    }

    @Test
    void activePlaybackCapturesPositiveResumePosition() {
        assertEquals(4200L, Mp4VideoSoundEngineLifecyclePolicy.resolveResumePosition(true, -1L, 4200L));
    }

    @Test
    void pausedPlaybackRetainsTheSameResumeBehavior() {
        assertTrue(Mp4VideoSoundEngineLifecyclePolicy.shouldCaptureResumePosition(true, -1L));
        assertEquals(4200L, Mp4VideoSoundEngineLifecyclePolicy.resolveResumePosition(true, -1L, 4200L));
    }

    @Test
    void explicitPendingSeekTakesPrecedenceOverReportedPosition() {
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldCaptureResumePosition(true, 8000L));
        assertEquals(8000L, Mp4VideoSoundEngineLifecyclePolicy.resolveResumePosition(true, 8000L, 4200L));
    }

    @Test
    void stoppedPlaybackDoesNotCaptureOrRetry() {
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldCaptureResumePosition(false, -1L));
        assertEquals(-1L, Mp4VideoSoundEngineLifecyclePolicy.resolveResumePosition(false, -1L, 4200L));
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldRetryPlayer(false, false, false));
    }

    @Test
    void retryRequiresOpenResourceWithPlaybackIntentAndNoPlayer() {
        assertTrue(Mp4VideoSoundEngineLifecyclePolicy.shouldRetryPlayer(false, true, false));
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldRetryPlayer(true, true, false));
        assertFalse(Mp4VideoSoundEngineLifecyclePolicy.shouldRetryPlayer(false, true, true));
    }
}
