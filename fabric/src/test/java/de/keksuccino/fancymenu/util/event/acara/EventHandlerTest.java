package de.keksuccino.fancymenu.util.event.acara;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void unregisterRemovesOnlyTheExactObjectInstance() {
        EventHandler handler = new EventHandler();
        CountingListener first = new CountingListener();
        CountingListener second = new CountingListener();
        handler.registerListenersOf(first);
        handler.registerListenersOf(second);

        handler.unregisterListenersOf(first);
        handler.postEvent(new TestEvent());

        assertEquals(0, first.calls.get());
        assertEquals(1, second.calls.get());
        assertTrue(handler.eventsRegisteredForType(TestEvent.class));

        handler.unregisterListenersOf(second);

        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void unregisterDuringDispatchAffectsOnlyTheNextEvent() {
        EventHandler handler = new EventHandler();
        CountingListener removed = new CountingListener();
        RemovingListener remover = new RemovingListener(handler, removed);
        handler.registerListenersOf(remover);
        handler.registerListenersOf(removed);

        handler.postEvent(new TestEvent());
        handler.postEvent(new TestEvent());

        assertEquals(2, remover.calls.get());
        assertEquals(1, removed.calls.get());
    }

    @Test
    void registrationDuringDispatchAffectsOnlyTheNextEvent() {
        EventHandler handler = new EventHandler();
        CountingListener added = new CountingListener();
        AddingListener adder = new AddingListener(handler, added);
        handler.registerListenersOf(adder);

        handler.postEvent(new TestEvent());
        assertEquals(0, added.calls.get());

        handler.postEvent(new TestEvent());
        assertEquals(1, added.calls.get());
    }

    @Test
    void listenersRunFromHighestToLowestPriority() {
        EventHandler handler = new EventHandler();
        List<String> order = new ArrayList<>();
        handler.registerListenersOf(new PrioritizedListeners(order));

        handler.postEvent(new TestEvent());

        assertEquals(List.of("high", "normal", "low"), order);
    }

    static final class CountingListener {

        private final AtomicInteger calls = new AtomicInteger();

        @EventListener
        public void onEvent(TestEvent event) {
            this.calls.incrementAndGet();
        }
    }

    static final class RemovingListener {

        private final EventHandler handler;
        private final Object removed;
        private final AtomicInteger calls = new AtomicInteger();

        RemovingListener(EventHandler handler, Object removed) {
            this.handler = handler;
            this.removed = removed;
        }

        @EventListener(priority = EventPriority.VERY_HIGH)
        public void onEvent(TestEvent event) {
            this.calls.incrementAndGet();
            this.handler.unregisterListenersOf(this.removed);
        }
    }

    static final class AddingListener {

        private final EventHandler handler;
        private final Object added;
        private boolean registered;

        AddingListener(EventHandler handler, Object added) {
            this.handler = handler;
            this.added = added;
        }

        @EventListener
        public void onEvent(TestEvent event) {
            if (!this.registered) {
                this.registered = true;
                this.handler.registerListenersOf(this.added);
            }
        }
    }

    static final class PrioritizedListeners {

        private final List<String> order;

        PrioritizedListeners(List<String> order) {
            this.order = order;
        }

        @EventListener(priority = EventPriority.LOW)
        public void low(TestEvent event) {
            this.order.add("low");
        }

        @EventListener
        public void normal(TestEvent event) {
            this.order.add("normal");
        }

        @EventListener(priority = EventPriority.HIGH)
        public void high(TestEvent event) {
            this.order.add("high");
        }
    }

    static final class TestEvent extends EventBase {

        @Override
        public boolean isCancelable() {
            return false;
        }
    }
}
