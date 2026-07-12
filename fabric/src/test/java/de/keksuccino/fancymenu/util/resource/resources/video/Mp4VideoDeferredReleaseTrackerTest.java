package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoDeferredReleaseTrackerTest {

    @Test
    void soundReloadClaimInvalidatesQueuedDeferredCleanup() {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object player = new Object();
        long version = tracker.schedule(player);

        assertSame(player, tracker.claimForSoundEngineReload(new Object(), () -> null).closeDetachedPlayer());
        assertEquals(Mp4VideoDeferredReleaseTracker.ClaimResult.LOST, tracker.claimAndReleaseWhenReady(player, version, () -> true, () -> {}));
        assertNull(tracker.claimForSoundEngineReload(new Object(), () -> null).closeDetachedPlayer());
    }

    @Test
    void soundReloadClaimsActiveAndCloseDetachedPlayersTogether() {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object activePlayer = new Object();
        Object closeDetachedPlayer = new Object();
        long closeVersion = tracker.schedule(closeDetachedPlayer);

        Mp4VideoDeferredReleaseTracker.SoundReloadClaim claim = tracker.claimForSoundEngineReload(new Object(), () -> activePlayer);

        assertSame(activePlayer, claim.activePlayer());
        assertSame(closeDetachedPlayer, claim.closeDetachedPlayer());
        assertFalse(tracker.isOwned(closeDetachedPlayer, closeVersion));
        assertFalse(tracker.hasPendingPlayer());
    }

    @Test
    void deferredReleaseClaimsPlayerExactlyOnce() {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object player = new Object();
        AtomicInteger releaseCount = new AtomicInteger();
        long version = tracker.schedule(player);

        assertEquals(Mp4VideoDeferredReleaseTracker.ClaimResult.DEFER, tracker.claimAndReleaseWhenReady(player, version, () -> false, releaseCount::incrementAndGet));
        assertEquals(Mp4VideoDeferredReleaseTracker.ClaimResult.CLAIMED, tracker.claimAndReleaseWhenReady(player, version, () -> true, releaseCount::incrementAndGet));
        assertEquals(Mp4VideoDeferredReleaseTracker.ClaimResult.LOST, tracker.claimAndReleaseWhenReady(player, version, () -> true, releaseCount::incrementAndGet));
        assertEquals(1, releaseCount.get());
        assertNull(tracker.claimForSoundEngineReload(new Object(), () -> null).closeDetachedPlayer());
    }

    @Test
    void cleanupFailureStillInvalidatesOwnership() {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object player = new Object();
        long version = tracker.schedule(player);

        assertThrows(IllegalStateException.class, () -> tracker.claimAndReleaseWhenReady(player, version, () -> true, () -> { throw new IllegalStateException("release"); }));
        assertFalse(tracker.hasPendingPlayer());
        assertNull(tracker.claimForSoundEngineReload(new Object(), () -> null).closeDetachedPlayer());
    }

    @Test
    void reloadClaimWaitsUntilInProgressDeferredReleaseFinishes() throws Exception {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object player = new Object();
        long version = tracker.schedule(player);
        CountDownLatch releaseEntered = new CountDownLatch(1);
        CountDownLatch allowReleaseCompletion = new CountDownLatch(1);
        AtomicReference<Object> reloadClaim = new AtomicReference<>(player);
        CountDownLatch reloadClaimCompleted = new CountDownLatch(1);

        Thread deferredThread = daemonThread(() -> tracker.claimAndReleaseWhenReady(player, version, () -> true, () -> {
            releaseEntered.countDown();
            await(allowReleaseCompletion);
        }));
        deferredThread.start();
        assertTrue(releaseEntered.await(5L, TimeUnit.SECONDS));

        Thread reloadThread = daemonThread(() -> {
            reloadClaim.set(tracker.claimForSoundEngineReload(new Object(), () -> null).closeDetachedPlayer());
            reloadClaimCompleted.countDown();
        });
        reloadThread.start();
        assertFalse(reloadClaimCompleted.await(100L, TimeUnit.MILLISECONDS));

        allowReleaseCompletion.countDown();
        deferredThread.join(5000L);
        reloadThread.join(5000L);
        assertFalse(deferredThread.isAlive());
        assertFalse(reloadThread.isAlive());
        assertNull(reloadClaim.get());
    }

    @Test
    void reloadClaimWaitsForCloseToScheduleDetachedPlayerUnderLifecycleLock() throws Exception {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object playerLifecycleLock = new Object();
        Object player = new Object();
        CountDownLatch closeOwnsLifecycleLock = new CountDownLatch(1);
        CountDownLatch allowCloseToSchedule = new CountDownLatch(1);
        CountDownLatch reloadClaimCompleted = new CountDownLatch(1);
        AtomicReference<Mp4VideoDeferredReleaseTracker.SoundReloadClaim> reloadClaim = new AtomicReference<>();

        Thread closeThread = daemonThread(() -> {
            synchronized (playerLifecycleLock) {
                closeOwnsLifecycleLock.countDown();
                await(allowCloseToSchedule);
                tracker.schedule(player);
            }
        });
        closeThread.start();
        assertTrue(closeOwnsLifecycleLock.await(5L, TimeUnit.SECONDS));

        Thread reloadThread = daemonThread(() -> {
            reloadClaim.set(tracker.claimForSoundEngineReload(playerLifecycleLock, () -> null));
            reloadClaimCompleted.countDown();
        });
        reloadThread.start();
        assertFalse(reloadClaimCompleted.await(100L, TimeUnit.MILLISECONDS));

        allowCloseToSchedule.countDown();
        closeThread.join(5000L);
        reloadThread.join(5000L);
        assertFalse(closeThread.isAlive());
        assertFalse(reloadThread.isAlive());
        assertSame(player, reloadClaim.get().closeDetachedPlayer());
    }

    @Test
    void onlyCurrentCloseRequestCanRunHardStopWork() {
        Mp4VideoDeferredReleaseTracker tracker = new Mp4VideoDeferredReleaseTracker();
        Object firstPlayer = new Object();
        Object secondPlayer = new Object();
        AtomicInteger hardStopCount = new AtomicInteger();
        long firstVersion = tracker.schedule(firstPlayer);
        long secondVersion = tracker.schedule(secondPlayer);

        assertFalse(tracker.runIfOwned(firstPlayer, firstVersion, hardStopCount::incrementAndGet));
        assertTrue(tracker.runIfOwned(secondPlayer, secondVersion, hardStopCount::incrementAndGet));
        assertEquals(1, hardStopCount.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test latch");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static Thread daemonThread(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
    }
}
