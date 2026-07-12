package de.keksuccino.fancymenu.customization.listener;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisionSafeListenerDispatchTest {

    @Test
    void inactiveListenerDoesNotSchedule() {
        TestListener listener = new TestListener();
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger deliveries = new AtomicInteger();

        assertFalse(RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet));
        assertTrue(scheduled.isEmpty());
        assertEquals(0, deliveries.get());
    }

    @Test
    void unchangedActiveRevisionDelivers() {
        TestListener listener = new TestListener();
        listener.registerInstance(createInstance(listener, "active"));
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger deliveries = new AtomicInteger();

        assertTrue(RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet));
        assertEquals(1, scheduled.size());

        scheduled.get(0).run();

        assertEquals(1, deliveries.get());
    }

    @Test
    void lifecycleAndReplacementChangesSuppressStaleDispatches() {
        TestListener listener = new TestListener();
        ListenerInstance first = createInstance(listener, "shared");
        ListenerInstance replacement = createInstance(listener, "shared");
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger deliveries = new AtomicInteger();

        listener.registerInstance(first);
        RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet);
        listener.unregisterInstance(first);
        scheduled.remove(0).run();

        listener.registerInstance(first);
        RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet);
        listener.registerInstance(replacement);
        scheduled.remove(0).run();

        RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet);
        listener.unregisterInstance(replacement);
        listener.registerInstance(replacement);
        scheduled.remove(0).run();

        assertEquals(0, deliveries.get());
    }

    @Test
    void identicalInstanceMapNoOpPreservesScheduledDelivery() {
        TestListener listener = new TestListener();
        ListenerInstance instance = createInstance(listener, "stable");
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger deliveries = new AtomicInteger();
        listener.registerInstance(instance);

        RevisionSafeListenerDispatch.scheduleIfActive(listener, scheduled::add, deliveries::incrementAndGet);
        listener.replaceInstances(List.of(instance));
        scheduled.get(0).run();

        assertEquals(1, deliveries.get());
    }

    private static ListenerInstance createInstance(AbstractListener listener, String identifier) {
        ListenerInstance instance = listener.createFreshInstance();
        instance.instanceIdentifier = identifier;
        return instance;
    }

    private static final class TestListener extends AbstractListener {

        private TestListener() {
            super("revision_safe_dispatch_test");
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.literal("test");
        }

        @Override
        public @NotNull List<Component> getDescription() {
            return List.of();
        }
    }
}
