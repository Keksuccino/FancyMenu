package de.keksuccino.fancymenu.util.event.acara;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void dispatchesByDescendingPriorityAndPreservesRegistrationOrderForTies() {
        EventHandler handler = new EventHandler();
        List<String> calls = new ArrayList<>();
        handler.registerListener(event -> calls.add("normal"), TestEvent.class, EventPriority.NORMAL);
        handler.registerListener(event -> calls.add("high-first"), TestEvent.class, EventPriority.HIGH);
        handler.registerListener(event -> calls.add("low"), TestEvent.class, EventPriority.LOW);
        handler.registerListener(event -> calls.add("high-second"), TestEvent.class, EventPriority.HIGH);

        handler.postEvent(new TestEvent());

        assertEquals(List.of("high-first", "high-second", "normal", "low"), calls);
    }

    @Test
    void unregistersOnlyTheExactAnnotatedListenerObject() {
        EventHandler handler = new EventHandler();
        EqualListener first = new EqualListener();
        EqualListener second = new EqualListener();
        handler.registerListenersOf(first);
        handler.registerListenersOf(second);

        handler.unregisterListenersOf(first);
        handler.postEvent(new TestEvent());

        assertEquals(0, first.calls.get());
        assertEquals(1, second.calls.get());
        assertTrue(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void unregisteringDuringDispatchAffectsOnlyTheNextEvent() {
        EventHandler handler = new EventHandler();
        SelfRemovingListener removing = new SelfRemovingListener(handler);
        EqualListener remaining = new EqualListener();
        handler.registerListenersOf(removing);
        handler.registerListenersOf(remaining);

        handler.postEvent(new TestEvent());
        handler.postEvent(new TestEvent());

        assertEquals(1, removing.calls.get());
        assertEquals(2, remaining.calls.get());
    }

    @Test
    void callbacksRunOutsideTheRegistryLock() throws Exception {
        EventHandler handler = new EventHandler();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicBoolean registrationCompletedInsideCallback = new AtomicBoolean();
        AtomicInteger lateListenerCalls = new AtomicInteger();
        handler.registerListener(event -> {
            Future<?> registration = executor.submit(() -> handler.registerListener(ignored -> lateListenerCalls.incrementAndGet(), TestEvent.class));
            try {
                registration.get(2, TimeUnit.SECONDS);
                registrationCompletedInsideCallback.set(true);
            } catch (Exception ignored) {
            }
        }, TestEvent.class, EventPriority.HIGH);

        try {
            handler.postEvent(new TestEvent());
            handler.postEvent(new TestEvent());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertTrue(registrationCompletedInsideCallback.get());
        assertEquals(1, lateListenerCalls.get());
    }

    @Test
    void concurrentRegistrationsPublishEveryListener() throws Exception {
        EventHandler handler = new EventHandler();
        int workerCount = 4;
        int listenersPerWorker = 500;
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<?>> registrations = new ArrayList<>();
        for (int worker = 0; worker < workerCount; worker++) {
            registrations.add(executor.submit(() -> {
                start.await();
                for (int listener = 0; listener < listenersPerWorker; listener++) {
                    handler.registerListener(event -> calls.incrementAndGet(), TestEvent.class);
                }
                return null;
            }));
        }

        try {
            start.countDown();
            for (Future<?> registration : registrations) {
                registration.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        handler.postEvent(new TestEvent());

        assertEquals(workerCount * listenersPerWorker, calls.get());
        assertFalse(handler.eventsRegisteredForType(null));
    }

    public static final class EqualListener {

        private final AtomicInteger calls = new AtomicInteger();

        @EventListener
        public void onEvent(TestEvent event) {
            this.calls.incrementAndGet();
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof EqualListener;
        }

        @Override
        public int hashCode() {
            return Objects.hash(EqualListener.class);
        }

    }

    public static final class SelfRemovingListener {

        private final EventHandler handler;
        private final AtomicInteger calls = new AtomicInteger();

        private SelfRemovingListener(EventHandler handler) {
            this.handler = handler;
        }

        @EventListener
        public void onEvent(TestEvent event) {
            this.calls.incrementAndGet();
            this.handler.unregisterListenersOf(this);
        }

    }

    public static final class TestEvent extends EventBase {

        @Override
        public boolean isCancelable() {
            return false;
        }

    }

}
