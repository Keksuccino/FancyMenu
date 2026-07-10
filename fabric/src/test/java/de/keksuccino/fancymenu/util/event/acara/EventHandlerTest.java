package de.keksuccino.fancymenu.util.event.acara;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void unregisterOnlyRemovesTheExactObjectInstance() {
        EventHandler handler = new EventHandler();
        CountingListener removed = new CountingListener();
        CountingListener retained = new CountingListener();
        handler.registerListenersOf(removed);
        handler.registerListenersOf(retained);

        handler.unregisterListenersOf(removed);
        handler.postEvent(new TestEvent());

        assertEquals(0, removed.invocations.get());
        assertEquals(1, retained.invocations.get());
        assertTrue(handler.eventsRegisteredForType(TestEvent.class));

        handler.unregisterListenersOf(retained);

        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void unregisteringDuringDispatchAffectsTheNextEvent() {
        EventHandler handler = new EventHandler();
        SelfRemovingListener listener = new SelfRemovingListener(handler);
        handler.registerListenersOf(listener);

        handler.postEvent(new TestEvent());
        handler.postEvent(new TestEvent());

        assertEquals(1, listener.invocations.get());
        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void unregisterDoesNotWaitForAnInFlightCallback() throws Exception {
        EventHandler handler = new EventHandler();
        BlockingListener listener = new BlockingListener();
        handler.registerListenersOf(listener);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> dispatch = executor.submit(() -> handler.postEvent(new TestEvent()));
            assertTrue(listener.callbackStarted.await(5, TimeUnit.SECONDS));

            Future<?> unregister = executor.submit(() -> handler.unregisterListenersOf(listener));
            unregister.get(5, TimeUnit.SECONDS);
            listener.allowCallbackToFinish.countDown();
            dispatch.get(5, TimeUnit.SECONDS);

            handler.postEvent(new TestEvent());

            assertEquals(1, listener.invocations.get());
            assertFalse(handler.eventsRegisteredForType(TestEvent.class));
        } finally {
            listener.allowCallbackToFinish.countDown();
            executor.shutdownNow();
        }
    }

    private static final class TestEvent extends EventBase {

        @Override
        public boolean isCancelable() {
            return false;
        }

    }

    public static class CountingListener {

        final AtomicInteger invocations = new AtomicInteger();

        @EventListener
        public void onEvent(TestEvent event) {
            this.invocations.incrementAndGet();
        }

    }

    public static final class SelfRemovingListener extends CountingListener {

        private final EventHandler handler;

        private SelfRemovingListener(EventHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onEvent(TestEvent event) {
            super.onEvent(event);
            this.handler.unregisterListenersOf(this);
        }

    }

    public static final class BlockingListener extends CountingListener {

        private final CountDownLatch callbackStarted = new CountDownLatch(1);
        private final CountDownLatch allowCallbackToFinish = new CountDownLatch(1);

        @Override
        public void onEvent(TestEvent event) {
            super.onEvent(event);
            this.callbackStarted.countDown();
            try {
                this.allowCallbackToFinish.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
