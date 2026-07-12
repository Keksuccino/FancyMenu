package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static de.keksuccino.fancymenu.util.resource.resources.video.Mp4VideoSoundLifecyclePolicy.PlayerCreationMode.NONE;
import static de.keksuccino.fancymenu.util.resource.resources.video.Mp4VideoSoundLifecyclePolicy.PlayerCreationMode.VIDEO_ONLY;
import static de.keksuccino.fancymenu.util.resource.resources.video.Mp4VideoSoundLifecyclePolicy.PlayerCreationMode.VIDEO_WITH_AUDIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoSoundLifecyclePolicyTest {

    @Test
    void waitsForInitialOpenAlAvailability() {
        assertEquals(NONE, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, true, false, false, false, false));
    }

    @Test
    void createsAudioPlayerWhenOpenAlIsReady() {
        assertEquals(VIDEO_WITH_AUDIO, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, true, false, false, true, false));
    }

    @Test
    void blocksCreationDuringReloadEvenWhenOpenAlStillReportsReady() {
        assertEquals(NONE, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, true, false, true, true, false));
    }

    @Test
    void fallsBackToVideoOnlyAfterCompletedReloadWithoutOpenAl() {
        assertEquals(VIDEO_ONLY, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, true, false, false, false, true));
    }

    @Test
    void doesNotCreateForStoppedClosedOrExistingPlayerStates() {
        assertEquals(NONE, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, false, false, false, true, true));
        assertEquals(NONE, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(true, true, false, false, true, true));
        assertEquals(NONE, Mp4VideoSoundLifecyclePolicy.determinePlayerCreationMode(false, true, true, false, true, true));
    }

    @Test
    void retriesPlayingIntentAfterReload() {
        assertTrue(Mp4VideoSoundLifecyclePolicy.shouldRetryAfterReload(false, true, false));
    }

    @Test
    void retriesPausedIntentBecausePauseRetainsThePlayRequest() {
        assertTrue(Mp4VideoSoundLifecyclePolicy.shouldRetryAfterReload(false, true, false));
    }

    @Test
    void doesNotRetryStoppedClosedOrExistingStates() {
        assertFalse(Mp4VideoSoundLifecyclePolicy.shouldRetryAfterReload(false, false, false));
        assertFalse(Mp4VideoSoundLifecyclePolicy.shouldRetryAfterReload(true, true, false));
        assertFalse(Mp4VideoSoundLifecyclePolicy.shouldRetryAfterReload(false, true, true));
    }

    @Test
    void explicitPendingSeekWinsOverCurrentPlaybackTime() {
        assertEquals(1250L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(true, 1250L, 9000L));
        assertEquals(0L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(true, 0L, 9000L));
    }

    @Test
    void currentPlaybackTimeIsPreservedOnlyForActiveIntentAndPositiveTime() {
        assertEquals(9000L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(true, -1L, 9000L));
        assertEquals(-1L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(true, -1L, 0L));
        assertEquals(-1L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(true, -1L, -50L));
        assertEquals(-1L, Mp4VideoSoundLifecyclePolicy.selectSeekForReload(false, 1250L, 9000L));
    }
}
