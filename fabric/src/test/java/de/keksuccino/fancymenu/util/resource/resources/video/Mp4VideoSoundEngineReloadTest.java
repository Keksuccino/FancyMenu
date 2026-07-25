package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mp4VideoSoundEngineReloadTest {

    @Test
    void activePlayerIsReleasedWithPlaybackPositionPreserved() {
        TestMp4Video video = new TestMp4Video();
        TestPlayer player = new TestPlayer(1432L);
        video.mediaPlayer = player;
        video.playRequested = true;
        video.seekRequestedMs = -1L;
        video.stopRequestVersion = 7L;
        video.framePresented = true;
        video.frameTexture.setHandle(42L);

        video.releasePlayerBeforeSoundEngineReload();

        assertNull(video.mediaPlayer);
        assertEquals(1432L, video.seekRequestedMs);
        assertEquals(8L, video.stopRequestVersion);
        assertFalse(video.framePresented);
        assertEquals(0, video.frameTexture.getId());
        assertTrue(player.paused);
        assertEquals(1, player.releaseCount);
    }

    @Test
    void explicitPendingSeekTakesPrecedenceOverReportedPosition() {
        TestMp4Video video = new TestMp4Video();
        TestPlayer player = new TestPlayer(1432L);
        video.mediaPlayer = player;
        video.playRequested = true;
        video.seekRequestedMs = 275L;

        video.releasePlayerBeforeSoundEngineReload();

        assertEquals(275L, video.seekRequestedMs);
        assertEquals(1, player.releaseCount);
    }

    @Test
    void retryPreservesPausedPlaybackIntent() {
        TestMp4Video video = new TestMp4Video();
        video.playRequested = true;
        video.pausedRequested = true;

        video.retryPlayerAfterSoundEngineReload();

        assertEquals(1, video.queuedInitializationCount);
        assertTrue(video.pausedRequested);
    }

    @Test
    void stoppedClosedAndAlreadyAttachedPlayersAreNotRetried() {
        TestMp4Video stoppedVideo = new TestMp4Video();
        stoppedVideo.retryPlayerAfterSoundEngineReload();

        TestMp4Video closedVideo = new TestMp4Video();
        closedVideo.playRequested = true;
        closedVideo.closed = true;
        closedVideo.retryPlayerAfterSoundEngineReload();

        TestMp4Video attachedVideo = new TestMp4Video();
        attachedVideo.playRequested = true;
        attachedVideo.mediaPlayer = new TestPlayer(0L);
        attachedVideo.retryPlayerAfterSoundEngineReload();

        assertEquals(0, stoppedVideo.queuedInitializationCount);
        assertEquals(0, closedVideo.queuedInitializationCount);
        assertEquals(0, attachedVideo.queuedInitializationCount);
    }

    @Test
    void pendingClosedPlayerIsClaimedBeforeContextReplacement() {
        TestMp4Video video = new TestMp4Video();
        TestPlayer player = new TestPlayer(0L);
        long releaseToken = video.closeReleaseController.schedule(player);
        video.closed = true;

        video.releasePlayerBeforeSoundEngineReload();
        video.runDeferredPlayerRelease(player, releaseToken, 0);

        assertTrue(player.paused);
        assertEquals(1, player.releaseCount);
        assertFalse(video.closeReleaseController.isScheduled(releaseToken));
        assertNull(video.closeReleaseController.claimForSoundEngineReload());
    }

    private static final class TestMp4Video extends Mp4Video {

        private int queuedInitializationCount;

        @Override
        protected void queuePlayerInitializationTask() {
            this.queuedInitializationCount++;
        }
    }

    public static final class TestPlayer {

        private final long time;
        private boolean paused;
        private int releaseCount;

        private TestPlayer(long time) {
            this.time = time;
        }

        public long time() {
            return this.time;
        }

        public boolean pause(boolean paused) {
            this.paused = paused;
            return true;
        }

        public void release() {
            this.releaseCount++;
        }
    }
}
