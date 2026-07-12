package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoPlayerCreationPolicyTest {

    @Test
    void waitsUntilPlaybackIsRequested() {
        assertFalse(Mp4VideoPlayerCreationPolicy.shouldCheckOpenAl(false, false));
        assertEquals(Mp4VideoPlayerCreationPolicy.Decision.WAIT, Mp4VideoPlayerCreationPolicy.decide(false, false, true, true));
    }

    @Test
    void waitsWhileSoundEngineIsReloadingEvenWhenOpenAlWasPreviouslyReady() {
        assertFalse(Mp4VideoPlayerCreationPolicy.shouldCheckOpenAl(true, true));
        assertEquals(Mp4VideoPlayerCreationPolicy.Decision.WAIT, Mp4VideoPlayerCreationPolicy.decide(true, true, true, true));
    }

    @Test
    void waitsForInitialSoundEngineReloadWhenOpenAlIsUnavailable() {
        assertEquals(Mp4VideoPlayerCreationPolicy.Decision.WAIT, Mp4VideoPlayerCreationPolicy.decide(true, false, false, false));
    }

    @Test
    void createsAudioBackedPlayerWhenOpenAlIsReady() {
        assertTrue(Mp4VideoPlayerCreationPolicy.shouldCheckOpenAl(true, false));
        assertEquals(Mp4VideoPlayerCreationPolicy.Decision.CREATE_WITH_AUDIO, Mp4VideoPlayerCreationPolicy.decide(true, false, false, true));
    }

    @Test
    void fallsBackToVideoOnlyAfterCompletedSoundReloadWithoutOpenAl() {
        assertEquals(Mp4VideoPlayerCreationPolicy.Decision.CREATE_VIDEO_ONLY, Mp4VideoPlayerCreationPolicy.decide(true, false, true, false));
    }
}
