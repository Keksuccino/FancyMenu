package de.keksuccino.fancymenu.util.event.acara;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHandlerTest {

    @Test
    void unregisterUsesObjectIdentity() {
        EventHandler handler = new EventHandler();
        CountingListener first = new CountingListener();
        CountingListener second = new CountingListener();
        handler.registerListenersOf(first);
        handler.registerListenersOf(second);

        handler.unregisterListenersOf(first);
        handler.postEvent(new TestEvent());

        assertEquals(0, first.calls);
        assertEquals(1, second.calls);
        assertTrue(handler.eventsRegisteredForType(TestEvent.class));
    }

    @Test
    void listenerCanUnregisterItselfDuringDispatch() {
        EventHandler handler = new EventHandler();
        SelfRemovingListener listener = new SelfRemovingListener(handler);
        handler.registerListenersOf(listener);

        handler.postEvent(new TestEvent());
        handler.postEvent(new TestEvent());

        assertEquals(1, listener.calls);
        assertFalse(handler.eventsRegisteredForType(TestEvent.class));
    }

    public static final class CountingListener {

        private int calls;

        @EventListener
        public void onEvent(TestEvent event) {
            this.calls++;
        }
    }

    public static final class SelfRemovingListener {

        private final EventHandler handler;
        private int calls;

        private SelfRemovingListener(EventHandler handler) {
            this.handler = handler;
        }

        @EventListener
        public void onEvent(TestEvent event) {
            this.calls++;
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
