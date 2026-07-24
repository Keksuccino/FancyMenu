package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenAudioPlaybackControllerTest {

    private static final long POLL_INTERVAL_NANOS = 100L;
    private static final long TIMEOUT_NANOS = 300L;

    @Test
    void readyAudioRestartsImmediatelyWithoutScheduling() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        TestAudio audio = TestAudio.ready();
        TestAudioSupplier supplier = new TestAudioSupplier(() -> audio);
        ScreenAudioPlaybackController controller = newController(scheduler, clock);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);

        assertEquals(List.of("stop", "play"), audio.operations);
        assertEquals(1, supplier.getCalls);
        assertEquals(0, scheduler.scheduleCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void slowAudioSelfSchedulesOnlyAfterEachCompletedPoll() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        TestAudio audio = new TestAudio();
        TestAudioSupplier supplier = new TestAudioSupplier(() -> audio);
        ScreenAudioPlaybackController controller = newController(scheduler, clock);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);

        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(1, supplier.getCalls);
        assertTrue(controller.isPending());
        assertEquals(1, scheduler.scheduleCalls, "No additional poll may be queued while the client-thread callback is stalled");

        audio.ready = true;
        clock.set(POLL_INTERVAL_NANOS);
        scheduler.tasks.get(0).runEvenIfCancelled();

        assertEquals(1, scheduler.scheduleCalls);
        assertEquals(2, supplier.getCalls);
        assertEquals(List.of("stop", "play"), audio.operations);
        assertFalse(controller.isPending());
    }

    @Test
    void nullSupplierResultTerminatesWithoutScheduling() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudioSupplier supplier = new TestAudioSupplier(() -> null);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);

        assertEquals(1, supplier.getCalls);
        assertEquals(0, scheduler.scheduleCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void nonRetryableLoadingFailureTerminatesWithoutScheduling() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudio audio = new TestAudio();
        audio.loadingFailed = true;
        TestAudioSupplier supplier = new TestAudioSupplier(() -> audio);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.CLOSE, supplier, null);

        assertFalse(audio.isLoadingFailureRetryable());
        assertEquals(0, scheduler.scheduleCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void retryableLoadingFailureCanBeReplacedByReadyAudio() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        TestAudio retryableFailure = new TestAudio();
        retryableFailure.loadingFailed = true;
        retryableFailure.retryableFailure = true;
        TestAudio readyReplacement = TestAudio.ready();
        TestAudio[] current = {retryableFailure};
        TestAudioSupplier supplier = new TestAudioSupplier(() -> current[0]);
        ScreenAudioPlaybackController controller = newController(scheduler, clock);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);
        assertTrue(controller.isPending());
        assertEquals(1, scheduler.scheduleCalls);

        current[0] = readyReplacement;
        clock.set(POLL_INTERVAL_NANOS);
        scheduler.tasks.get(0).runEvenIfCancelled();

        assertEquals(List.of("stop", "play"), readyReplacement.operations);
        assertFalse(controller.isPending());
    }

    @Test
    void readyStateWinsOverSimultaneousFailureFlags() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudio audio = TestAudio.ready();
        audio.loadingFailed = true;
        TestAudioSupplier supplier = new TestAudioSupplier(() -> audio);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);

        assertEquals(List.of("stop", "play"), audio.operations);
        assertEquals(0, scheduler.scheduleCalls);
    }

    @Test
    void completedButNotReadyAndClosedResourcesAreTerminal() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudio completed = new TestAudio();
        completed.loadingCompleted = true;
        TestAudio closed = new TestAudio();
        closed.closed = true;
        ScreenAudioPlaybackController firstController = newController(scheduler, new MutableClock());
        ScreenAudioPlaybackController secondController = newController(scheduler, new MutableClock());

        firstController.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, new TestAudioSupplier(() -> completed), null);
        secondController.playWhenReady(ScreenAudioPlaybackController.Cue.CLOSE, new TestAudioSupplier(() -> closed), null);

        assertEquals(0, scheduler.scheduleCalls);
        assertFalse(firstController.isPending());
        assertFalse(secondController.isPending());
    }

    @Test
    void timeoutStopsAtExactDeadlineWithoutReadingSupplierAgain() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        TestAudioSupplier supplier = new TestAudioSupplier(TestAudio::new);
        ScreenAudioPlaybackController controller = newController(scheduler, clock);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);
        clock.set(TIMEOUT_NANOS);
        scheduler.tasks.get(0).runEvenIfCancelled();

        assertEquals(1, supplier.getCalls);
        assertEquals(1, scheduler.scheduleCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void lastPollIsClippedToTheRemainingDeadline() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        TestAudio audio = new TestAudio();
        ScreenAudioPlaybackController controller = new ScreenAudioPlaybackController(scheduler, clock, POLL_INTERVAL_NANOS, 250L);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, new TestAudioSupplier(() -> audio), null);
        assertEquals(100L, scheduler.tasks.get(0).delayNanos);

        clock.set(100L);
        scheduler.tasks.get(0).runEvenIfCancelled();
        assertEquals(100L, scheduler.tasks.get(1).delayNanos);

        clock.set(200L);
        scheduler.tasks.get(1).runEvenIfCancelled();
        assertEquals(50L, scheduler.tasks.get(2).delayNanos);
    }

    @Test
    void cancellationCancelsHandleAndMakesQueuedCallbackHarmless() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudioSupplier supplier = new TestAudioSupplier(TestAudio::new);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, null);
        ManualTask staleTask = scheduler.tasks.get(0);
        controller.cancel();

        assertEquals(1, staleTask.cancelCalls);
        staleTask.runEvenIfCancelled();
        assertEquals(1, supplier.getCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void newerCueSupersedesTheOldGeneration() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudioSupplier oldSupplier = new TestAudioSupplier(TestAudio::new);
        TestAudio newAudio = TestAudio.ready();
        TestAudioSupplier newSupplier = new TestAudioSupplier(() -> newAudio);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.CLOSE, oldSupplier, null);
        ManualTask staleTask = scheduler.tasks.get(0);
        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, newSupplier, null);

        assertEquals(1, staleTask.cancelCalls);
        staleTask.runEvenIfCancelled();
        assertEquals(1, oldSupplier.getCalls);
        assertEquals(List.of("stop", "play"), newAudio.operations);
    }

    @Test
    void cancellationCannotReturnBeforeAlreadyClaimedPlaybackFinishes() throws Exception {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudio audio = TestAudio.ready();
        audio.stopEntered = new CountDownLatch(1);
        audio.releaseStop = new CountDownLatch(1);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());
        AtomicBoolean cancellationReturned = new AtomicBoolean();
        List<Thread> spawnedThreads = new ArrayList<>();

        Thread playbackThread = startThread(() -> controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, new TestAudioSupplier(() -> audio), null));
        spawnedThreads.add(playbackThread);
        Thread cancellationThread;
        try {
            assertTrue(audio.stopEntered.await(5L, TimeUnit.SECONDS));
            cancellationThread = startThread(() -> {
                controller.cancel();
                cancellationReturned.set(true);
            });
            spawnedThreads.add(cancellationThread);

            awaitBlocked(cancellationThread);
            assertFalse(cancellationReturned.get());
        } finally {
            audio.releaseStop.countDown();
            joinOrInterrupt(spawnedThreads);
        }

        assertFalse(playbackThread.isAlive());
        assertFalse(cancellationThread.isAlive());
        assertTrue(cancellationReturned.get());
        assertEquals(List.of("stop", "play"), audio.operations);
    }

    @Test
    void newerReadyCueCannotOvertakeAlreadyClaimedPlayback() throws Exception {
        ManualScheduler scheduler = new ManualScheduler();
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());
        List<String> playbackOrder = Collections.synchronizedList(new ArrayList<>());
        TestAudio oldAudio = TestAudio.ready();
        oldAudio.operationPrefix = "old:";
        oldAudio.sharedOperations = playbackOrder;
        oldAudio.stopEntered = new CountDownLatch(1);
        oldAudio.releaseStop = new CountDownLatch(1);
        TestAudio newAudio = TestAudio.ready();
        newAudio.operationPrefix = "new:";
        newAudio.sharedOperations = playbackOrder;
        List<Thread> spawnedThreads = new ArrayList<>();

        Thread oldPlaybackThread = startThread(() -> controller.playWhenReady(ScreenAudioPlaybackController.Cue.CLOSE, new TestAudioSupplier(() -> oldAudio), null));
        spawnedThreads.add(oldPlaybackThread);
        Thread newPlaybackThread;
        try {
            assertTrue(oldAudio.stopEntered.await(5L, TimeUnit.SECONDS));
            newPlaybackThread = startThread(() -> controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, new TestAudioSupplier(() -> newAudio), null));
            spawnedThreads.add(newPlaybackThread);

            awaitBlocked(newPlaybackThread);
        } finally {
            oldAudio.releaseStop.countDown();
            joinOrInterrupt(spawnedThreads);
        }

        assertFalse(oldPlaybackThread.isAlive());
        assertFalse(newPlaybackThread.isAlive());
        assertEquals(List.of("old:stop", "old:play", "new:stop", "new:play"), playbackOrder);
    }

    @Test
    void configurationRetentionUsesExactSupplierAndOwnerIdentity() {
        ManualScheduler scheduler = new ManualScheduler();
        Layout owner = new Layout("test-screen");
        TestAudioSupplier supplier = new TestAudioSupplier(TestAudio::new);
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, owner);
        ManualTask task = scheduler.tasks.get(0);
        controller.retainConfigured(supplier, owner, null, null);
        assertTrue(controller.isPending());
        assertEquals(0, task.cancelCalls);

        controller.retainConfigured(new TestAudioSupplier(TestAudio::new), owner, null, null);
        assertFalse(controller.isPending());
        assertEquals(1, task.cancelCalls);
    }

    @Test
    void exactOwnerRetirementCancelsOnlyItsAttempt() {
        ManualScheduler scheduler = new ManualScheduler();
        Layout owner = new Layout("test-screen");
        Layout unrelated = new Layout("test-screen");
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.CLOSE, new TestAudioSupplier(TestAudio::new), owner);
        ManualTask task = scheduler.tasks.get(0);
        controller.cancelIfOwnedBy(List.of(unrelated));
        assertTrue(controller.isPending());
        assertEquals(0, task.cancelCalls);

        controller.cancelIfOwnedBy(List.of(owner));
        assertFalse(controller.isPending());
        assertEquals(1, task.cancelCalls);
    }

    @Test
    void destroyedOwnerStopsBeforeAnotherSupplierRead() {
        ManualScheduler scheduler = new ManualScheduler();
        MutableClock clock = new MutableClock();
        Layout owner = new Layout("test-screen");
        TestAudioSupplier supplier = new TestAudioSupplier(TestAudio::new);
        ScreenAudioPlaybackController controller = newController(scheduler, clock);

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, supplier, owner);
        owner.destroy();
        clock.set(POLL_INTERVAL_NANOS);
        scheduler.tasks.get(0).runEvenIfCancelled();

        assertEquals(1, supplier.getCalls);
        assertFalse(controller.isPending());
    }

    @Test
    void closeIsTerminalAndRejectsLaterRequests() {
        ManualScheduler scheduler = new ManualScheduler();
        TestAudioSupplier firstSupplier = new TestAudioSupplier(TestAudio::new);
        TestAudioSupplier rejectedSupplier = new TestAudioSupplier(() -> TestAudio.ready());
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, firstSupplier, null);
        ManualTask task = scheduler.tasks.get(0);
        controller.close();
        controller.close();
        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, rejectedSupplier, null);

        assertTrue(controller.isClosed());
        assertFalse(controller.isPending());
        assertEquals(1, task.cancelCalls);
        assertEquals(0, rejectedSupplier.getCalls);
    }

    @Test
    void schedulerRejectionTerminatesAttempt() {
        ManualScheduler scheduler = new ManualScheduler();
        scheduler.failureCall = 1;
        ScreenAudioPlaybackController controller = newController(scheduler, new MutableClock());

        controller.playWhenReady(ScreenAudioPlaybackController.Cue.OPEN, new TestAudioSupplier(TestAudio::new), null);

        assertEquals(1, scheduler.scheduleCalls);
        assertFalse(controller.isPending());
    }

    private static ScreenAudioPlaybackController newController(ManualScheduler scheduler, MutableClock clock) {
        return new ScreenAudioPlaybackController(scheduler, clock, POLL_INTERVAL_NANOS, TIMEOUT_NANOS);
    }

    private static Thread startThread(@NotNull Runnable task) {
        Thread thread = new Thread(task);
        thread.start();
        return thread;
    }

    private static void awaitBlocked(@NotNull Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(Thread.State.BLOCKED, thread.getState());
    }

    private static void joinOrInterrupt(@NotNull List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) thread.join(5000L);
        for (Thread thread : threads) {
            if (thread.isAlive()) thread.interrupt();
        }
        for (Thread thread : threads) {
            if (thread.isAlive()) thread.join(5000L);
        }
    }

    private static final class MutableClock implements java.util.function.LongSupplier {

        private final AtomicLong value = new AtomicLong();

        @Override
        public long getAsLong() {
            return this.value.get();
        }

        private void set(long value) {
            this.value.set(value);
        }
    }

    private static final class ManualScheduler implements ScreenAudioPlaybackController.Scheduler {

        private final List<ManualTask> tasks = new ArrayList<>();
        private int scheduleCalls;
        private int failureCall = -1;

        @Override
        public ScreenAudioPlaybackController.ScheduledTask schedule(Runnable task, long delayNanos) {
            this.scheduleCalls++;
            if (this.scheduleCalls == this.failureCall) throw new RejectedExecutionException("scheduler stopped");
            ManualTask scheduledTask = new ManualTask(task, delayNanos);
            this.tasks.add(scheduledTask);
            return scheduledTask;
        }
    }

    private static final class ManualTask implements ScreenAudioPlaybackController.ScheduledTask {

        private final Runnable command;
        private final long delayNanos;
        private int cancelCalls;

        private ManualTask(Runnable command, long delayNanos) {
            this.command = command;
            this.delayNanos = delayNanos;
        }

        @Override
        public void cancel() {
            this.cancelCalls++;
        }

        private void runEvenIfCancelled() {
            this.command.run();
        }
    }

    private static final class TestAudioSupplier extends ResourceSupplier<IAudio> {

        private final Supplier<IAudio> delegate;
        private int getCalls;

        private TestAudioSupplier(@NotNull Supplier<IAudio> delegate) {
            super(IAudio.class, FileMediaType.AUDIO, "test.ogg");
            this.delegate = delegate;
        }

        @Override
        public @Nullable IAudio get() {
            this.getCalls++;
            return this.delegate.get();
        }
    }

    private static final class TestAudio implements IAudio {

        private final List<String> operations = new ArrayList<>();
        private boolean ready;
        private boolean loadingCompleted;
        private boolean loadingFailed;
        private boolean retryableFailure;
        private boolean closed;
        private boolean playing;
        private boolean paused;
        private float volume;
        private SoundSource soundSource = SoundSource.MASTER;
        private String operationPrefix = "";
        @Nullable
        private List<String> sharedOperations;
        @Nullable
        private CountDownLatch stopEntered;
        @Nullable
        private CountDownLatch releaseStop;

        private static TestAudio ready() {
            TestAudio audio = new TestAudio();
            audio.ready = true;
            audio.loadingCompleted = true;
            return audio;
        }

        @Override
        public boolean isLoadingFailureRetryable() {
            return this.retryableFailure;
        }

        @Override
        public void play() {
            this.operations.add("play");
            this.recordSharedOperation("play");
            this.playing = true;
            this.paused = false;
        }

        @Override
        public boolean isPlaying() {
            return this.playing;
        }

        @Override
        public void pause() {
            this.paused = true;
            this.playing = false;
        }

        @Override
        public boolean isPaused() {
            return this.paused;
        }

        @Override
        public void stop() {
            this.operations.add("stop");
            this.recordSharedOperation("stop");
            if (this.stopEntered != null) this.stopEntered.countDown();
            if (this.releaseStop != null) {
                try {
                    this.releaseStop.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted while waiting to release test audio", ex);
                }
            }
            this.playing = false;
            this.paused = false;
        }

        @Override
        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public float getVolume() {
            return this.volume;
        }

        @Override
        public void setSoundChannel(@NotNull SoundSource channel) {
            this.soundSource = channel;
        }

        @Override
        public @NotNull SoundSource getSoundChannel() {
            return this.soundSource;
        }

        @Override
        public float getDuration() {
            return 0.0F;
        }

        @Override
        public float getPlayTime() {
            return 0.0F;
        }

        @Override
        public @Nullable InputStream open() {
            return null;
        }

        @Override
        public boolean isReady() {
            return this.ready;
        }

        @Override
        public boolean isLoadingCompleted() {
            return this.loadingCompleted;
        }

        @Override
        public boolean isLoadingFailed() {
            return this.loadingFailed;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        private void recordSharedOperation(@NotNull String operation) {
            if (this.sharedOperations != null) this.sharedOperations.add(this.operationPrefix + operation);
        }
    }
}
