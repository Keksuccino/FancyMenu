package de.keksuccino.fancymenu.util.resource.resources.video;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.watermedia.WatermediaDeferredPlayerReleaseTracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A closed video hands its player to two release owners: its own deferred release, which a sound engine reload can take over, and the
 * {@link WatermediaDeferredPlayerReleaseTracker}, which releases the players that still wait when the client shuts down.
 */
class Mp4VideoPlayerReleaseOwnershipTest {

    @AfterEach
    void dropTheDeferredReleaseTasksOfTheClosedVideos() {
        // Closing a video queues its deferred release for the next client ticks, which never come in unit tests
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    @Test
    void aSoundEngineReloadReleasesThePlayerOfAClosedVideoOnlyOnce() {
        Mp4Video video = new Mp4Video();
        FakePlayer player = new FakePlayer();
        video.mediaPlayer = player;

        video.close();
        // Like the release of a sound engine reload, which happens before the deferred release of the close got its turn
        video.releasePlayerBeforeSoundEngineReload();

        assertEquals(1, player.releases.get());
        // Neither the deferred release of the close nor the client shutdown may release the player a second time
        WatermediaDeferredPlayerReleaseTracker.release(player);
        assertEquals(1, player.releases.get());
    }

    @Test
    void aSoundEngineReloadNeverReleasesAPlayerThatTheDeferredReleaseTrackerReleasedAlready() {
        Mp4Video video = new Mp4Video();
        FakePlayer player = new FakePlayer();
        video.mediaPlayer = player;

        video.close();
        WatermediaDeferredPlayerReleaseTracker.release(player);
        video.releasePlayerBeforeSoundEngineReload();

        assertEquals(1, player.releases.get());
    }

    /** Stands in for a WaterMedia player, which the reflection bridge controls through its public methods. */
    public static final class FakePlayer {

        private final AtomicInteger releases = new AtomicInteger();

        public void pause(boolean paused) {
        }

        public void stop() {
        }

        public void release() {
            this.releases.incrementAndGet();
        }

    }

}
