package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorHandleLifecycleTest {

    @Test
    void destructionRunsOnceOnGlfwThread() {
        Fixture fixture = new Fixture(true);
        CursorHandleLifecycle.Handle cursor = fixture.lifecycle.trackCustom(7L);

        fixture.lifecycle.destroy(cursor);
        fixture.lifecycle.destroy(cursor);

        assertEquals(List.of("switch:7", "destroy:7"), fixture.nativeOperations.events);
        assertFalse(cursor.isLive());
        assertEquals(0, fixture.tasks.size());
        assertTrue(fixture.errors.isEmpty());
    }

    @Test
    void offThreadDestructionIsQueuedUntilGlfwThreadRunsIt() {
        Fixture fixture = new Fixture(false);
        CursorHandleLifecycle.Handle cursor = fixture.lifecycle.trackCustom(9L);

        fixture.lifecycle.destroy(cursor);

        assertEquals(1, fixture.tasks.size());
        assertTrue(fixture.nativeOperations.events.isEmpty());
        fixture.runNextOnGlfwThread();
        assertEquals(List.of("switch:9", "destroy:9"), fixture.nativeOperations.events);
        assertTrue(fixture.errors.isEmpty());
    }

    @Test
    void concurrentDestroyRequestsScheduleExactlyOneNativeRelease() throws Exception {
        Fixture fixture = new Fixture(false);
        CursorHandleLifecycle.Handle cursor = fixture.lifecycle.trackCustom(11L);

        ConcurrentTestCalls.invoke(32, () -> {
            fixture.lifecycle.destroy(cursor);
            return null;
        });

        assertEquals(1, fixture.tasks.size());
        fixture.runNextOnGlfwThread();
        assertEquals(List.of("switch:11", "destroy:11"), fixture.nativeOperations.events);
    }

    @Test
    void shutdownPreparesWindowThenDestroysCustomBeforeStandardHandles() {
        Fixture fixture = new Fixture(true);
        fixture.lifecycle.trackStandard(101L);
        fixture.lifecycle.trackStandard(102L);
        fixture.lifecycle.trackCustom(12L);
        fixture.lifecycle.trackCustom(13L);

        fixture.lifecycle.shutdown();
        fixture.lifecycle.shutdown();

        assertEquals("prepare", fixture.nativeOperations.events.get(0));
        assertEquals(List.of("switch:12", "destroy:12", "switch:13", "destroy:13"), fixture.nativeOperations.events.subList(1, 5));
        assertEquals(List.of("switch:101", "destroy:101", "switch:102", "destroy:102"), fixture.nativeOperations.events.subList(5, 9));
        assertTrue(fixture.errors.isEmpty());
    }

    @Test
    void shutdownDrainsRequestedHandleAndStaleTaskCannotDestroyReusedNumericHandle() {
        Fixture fixture = new Fixture(false);
        CursorHandleLifecycle.Handle oldAllocation = fixture.lifecycle.trackCustom(21L);
        fixture.lifecycle.destroy(oldAllocation);
        assertEquals(1, fixture.tasks.size());

        fixture.onGlfwThread.set(true);
        fixture.lifecycle.shutdown();
        assertEquals(1, fixture.nativeOperations.destroyCount(21L));

        // Simulate GLFW reusing 21 for a newer allocation before the delayed old task reaches the queue.
        fixture.tasks.remove().run();

        assertEquals(1, fixture.nativeOperations.destroyCount(21L));
        assertTrue(fixture.errors.isEmpty());
    }

    @Test
    void zeroAndFailedHandlesNeverReachNativeDestroy() {
        Fixture fixture = new Fixture(true);
        CursorHandleLifecycle.Handle zero = fixture.lifecycle.trackCustom(0L);
        fixture.lifecycle.trackStandard(0L);

        fixture.lifecycle.destroy(zero);
        fixture.lifecycle.shutdown();

        assertEquals(List.of("prepare"), fixture.nativeOperations.events);
        assertFalse(zero.wasAccepted());
        assertTrue(fixture.errors.isEmpty());
    }

    @Test
    void failedNativeDestroyReturnsAllocationToRetryableState() {
        Fixture fixture = new Fixture(true);
        CursorHandleLifecycle.Handle cursor = fixture.lifecycle.trackCustom(31L);
        fixture.nativeOperations.failNextDestroy.set(true);

        fixture.lifecycle.destroy(cursor);

        assertTrue(cursor.isLive());
        assertEquals(1, fixture.errors.size());
        fixture.lifecycle.destroy(cursor);
        assertFalse(cursor.isLive());
        assertEquals(2, fixture.nativeOperations.switchCount(31L));
        assertEquals(1, fixture.nativeOperations.destroyCount(31L));
    }

    @Test
    void allocationsSubmittedAfterShutdownAreDestroyedInsteadOfRetained() {
        Fixture fixture = new Fixture(true);
        fixture.lifecycle.shutdown();

        CursorHandleLifecycle.Handle lateCustom = fixture.lifecycle.trackCustom(41L);
        fixture.lifecycle.trackStandard(42L);

        assertFalse(lateCustom.wasAccepted());
        assertEquals(1, fixture.nativeOperations.destroyCount(41L));
        assertEquals(1, fixture.nativeOperations.destroyCount(42L));
    }

    private static final class Fixture {

        private final AtomicBoolean onGlfwThread = new AtomicBoolean();
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final RecordingNativeOperations nativeOperations = new RecordingNativeOperations();
        private final List<Throwable> errors = new ArrayList<>();
        private final CursorHandleLifecycle lifecycle = new CursorHandleLifecycle(new CursorHandleLifecycle.ThreadExecutor() {
            @Override
            public boolean isOnThread() {
                return Fixture.this.onGlfwThread.get();
            }

            @Override
            public void execute(Runnable task) {
                if (this.isOnThread()) task.run(); else Fixture.this.tasks.add(task);
            }
        }, this.nativeOperations, this.errors::add);

        private Fixture(boolean onGlfwThread) {
            this.onGlfwThread.set(onGlfwThread);
        }

        private void runNextOnGlfwThread() {
            Runnable task = this.tasks.remove();
            this.onGlfwThread.set(true);
            task.run();
        }

    }

    private static final class RecordingNativeOperations implements CursorHandleLifecycle.NativeOperations {

        private final List<String> events = new ArrayList<>();
        private final AtomicBoolean failNextDestroy = new AtomicBoolean();

        @Override
        public void prepareForShutdown() {
            this.events.add("prepare");
        }

        @Override
        public void destroyCursor(long nativeHandle) {
            this.events.add("switch:" + nativeHandle);
            if (this.failNextDestroy.compareAndSet(true, false)) throw new IllegalStateException("expected native failure");
            this.events.add("destroy:" + nativeHandle);
        }

        private long switchCount(long nativeHandle) {
            return this.events.stream().filter(event -> event.equals("switch:" + nativeHandle)).count();
        }

        private long destroyCount(long nativeHandle) {
            return this.events.stream().filter(event -> event.equals("destroy:" + nativeHandle)).count();
        }

    }

}
