package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakSoundEngineReloadCoordinatorTest {

    @Test
    void exposesPhaseFlagsBeforeDispatchingCallbacks() {
        AtomicReference<WeakSoundEngineReloadCoordinator<Object>> coordinatorReference = new AtomicReference<>();
        List<String> events = new ArrayList<>();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> {
            assertTrue(coordinatorReference.get().isReloading());
            assertFalse(coordinatorReference.get().hasReloadCompleted());
            events.add("before");
        }, participant -> {
            assertFalse(coordinatorReference.get().isReloading());
            assertTrue(coordinatorReference.get().hasReloadCompleted());
            events.add("after");
        }, (participant, phase, throwable) -> {});
        coordinatorReference.set(coordinator);
        Object participant = new Object();
        coordinator.register(participant);

        coordinator.beforeReload();
        coordinator.afterReload();

        assertEquals(List.of("before", "after"), events);
    }

    @Test
    void repeatedPhaseCallsAndReloadCyclesDispatchExactlyOncePerCycle() {
        AtomicInteger beforeCount = new AtomicInteger();
        AtomicInteger afterCount = new AtomicInteger();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> beforeCount.incrementAndGet(), participant -> afterCount.incrementAndGet(), (participant, phase, throwable) -> {});
        Object participant = new Object();
        coordinator.register(participant);

        coordinator.beforeReload();
        coordinator.beforeReload();
        coordinator.afterReload();
        coordinator.afterReload();
        coordinator.beforeReload();
        coordinator.afterReload();

        assertEquals(2, beforeCount.get());
        assertEquals(2, afterCount.get());
        assertFalse(coordinator.isReloading());
        assertTrue(coordinator.hasReloadCompleted());
    }

    @Test
    void registrationDuringReloadParticipatesInAfterPhase() {
        Object first = new Object();
        Object late = new Object();
        AtomicReference<WeakSoundEngineReloadCoordinator<Object>> coordinatorReference = new AtomicReference<>();
        List<Object> afterParticipants = new ArrayList<>();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> coordinatorReference.get().register(late), afterParticipants::add, (participant, phase, throwable) -> {});
        coordinatorReference.set(coordinator);
        coordinator.register(first);

        coordinator.beforeReload();
        coordinator.afterReload();

        assertEquals(List.of(first, late), afterParticipants);
    }

    @Test
    void callbackFailuresAreIsolatedPerParticipantAndPhase() {
        Object failing = new Object();
        Object healthy = new Object();
        AtomicInteger healthyBeforeCount = new AtomicInteger();
        AtomicInteger healthyAfterCount = new AtomicInteger();
        List<WeakSoundEngineReloadCoordinator.ReloadPhase> failures = new ArrayList<>();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> {
            if (participant == failing) throw new IllegalStateException("cleanup");
            healthyBeforeCount.incrementAndGet();
        }, participant -> {
            if (participant == failing) throw new IllegalStateException("retry");
            healthyAfterCount.incrementAndGet();
        }, (participant, phase, throwable) -> failures.add(phase));
        coordinator.register(failing);
        coordinator.register(healthy);

        coordinator.beforeReload();
        coordinator.afterReload();

        assertEquals(1, healthyBeforeCount.get());
        assertEquals(1, healthyAfterCount.get());
        assertEquals(List.of(WeakSoundEngineReloadCoordinator.ReloadPhase.BEFORE, WeakSoundEngineReloadCoordinator.ReloadPhase.AFTER), failures);
    }

    @Test
    void unregisterAndClearedWeakReferencesStopLifecycleCallbacks() {
        AtomicReference<WeakReference<Object>> capturedReference = new AtomicReference<>();
        AtomicInteger callbackCount = new AtomicInteger();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> callbackCount.incrementAndGet(), participant -> callbackCount.incrementAndGet(), (participant, phase, throwable) -> {}, participant -> {
            WeakReference<Object> reference = new WeakReference<>(participant);
            capturedReference.set(reference);
            return reference;
        });
        Object cleared = new Object();
        coordinator.register(cleared);
        capturedReference.get().clear();
        coordinator.beforeReload();
        coordinator.afterReload();
        assertEquals(0, callbackCount.get());

        Object unregistered = new Object();
        coordinator.register(unregistered);
        coordinator.unregister(unregistered);
        coordinator.beforeReload();
        coordinator.afterReload();
        assertEquals(0, callbackCount.get());
    }

    @Test
    void reentrantAfterWaitsForBeforeDispatchToFinish() {
        AtomicReference<WeakSoundEngineReloadCoordinator<Object>> coordinatorReference = new AtomicReference<>();
        List<String> events = new ArrayList<>();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> {
            events.add("before-start");
            coordinatorReference.get().beforeReload();
            coordinatorReference.get().afterReload();
            events.add("before-end");
        }, participant -> events.add("after"), (participant, phase, throwable) -> {});
        coordinatorReference.set(coordinator);
        Object participant = new Object();
        coordinator.register(participant);

        coordinator.beforeReload();

        assertEquals(List.of("before-start", "before-end", "after"), events);
        assertFalse(coordinator.isReloading());
        assertTrue(coordinator.hasReloadCompleted());
    }

    @Test
    void concurrentAfterCannotOvertakeBlockedBeforeCallback() throws Exception {
        CountDownLatch beforeEntered = new CountDownLatch(1);
        CountDownLatch allowBeforeCompletion = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        WeakSoundEngineReloadCoordinator<Object> coordinator = new WeakSoundEngineReloadCoordinator<>(participant -> {
            events.add("before-start");
            beforeEntered.countDown();
            await(allowBeforeCompletion);
            events.add("before-end");
        }, participant -> events.add("after"), (participant, phase, throwable) -> {});
        Object participant = new Object();
        coordinator.register(participant);
        Thread beforeThread = daemonThread(coordinator::beforeReload);
        beforeThread.start();
        assertTrue(beforeEntered.await(5L, TimeUnit.SECONDS));

        Thread afterThread = daemonThread(coordinator::afterReload);
        afterThread.start();
        afterThread.join(5000L);
        assertFalse(afterThread.isAlive());
        assertEquals(List.of("before-start"), events);
        assertTrue(coordinator.isReloading());

        allowBeforeCompletion.countDown();
        beforeThread.join(5000L);
        assertFalse(beforeThread.isAlive());
        assertEquals(List.of("before-start", "before-end", "after"), events);
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
