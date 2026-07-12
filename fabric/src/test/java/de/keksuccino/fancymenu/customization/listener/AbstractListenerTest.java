package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractListenerTest {

    @Test
    void lifecycleHooksFireOnlyForActiveBoundaryTransitions() {
        TestListener listener = new TestListener();
        ListenerInstance first = instance(listener, "first");
        ListenerInstance second = instance(listener, "second");

        listener.registerInstance(first);
        long firstRevision = listener.getActiveInstanceRevision();
        listener.registerInstance(first);
        listener.registerInstance(second);
        long secondRevision = listener.getActiveInstanceRevision();
        listener.unregisterInstance("missing");
        listener.unregisterInstance(first);

        assertEquals(1, listener.activationCount);
        assertEquals(0, listener.deactivationCount);
        assertTrue(listener.hasInstancesListening());
        assertTrue(listener.isActiveAtRevision(secondRevision + 1L));
        assertNotEquals(firstRevision, secondRevision);

        listener.unregisterInstance(second);

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());
        assertEquals(1, listener.deactivationCount);
        assertFalse(listener.isActiveAtRevision(secondRevision + 1L));
    }

    @Test
    void replaceInstancesIsAtomicAndInvalidatesPriorRevision() {
        TestListener listener = new TestListener();
        ListenerInstance first = instance(listener, "first");
        ListenerInstance replacement = instance(listener, "replacement");

        listener.replaceInstances(List.of(first));
        long firstRevision = listener.getActiveInstanceRevision();
        listener.replaceInstances(List.of(first));

        assertEquals(firstRevision, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activationCount);

        listener.replaceInstances(List.of(replacement));
        long replacementRevision = listener.getActiveInstanceRevision();

        assertFalse(listener.isActiveAtRevision(firstRevision));
        assertTrue(listener.isActiveAtRevision(replacementRevision));
        assertEquals(1, listener.activationCount);
        assertEquals(0, listener.deactivationCount);

        listener.replaceInstances(List.of());

        assertEquals(1, listener.deactivationCount);
        assertFalse(listener.hasInstancesListening());
    }

    @Test
    void registrationRejectsInstancesOwnedByAnotherProvider() {
        TestListener listener = new TestListener();
        ListenerInstance foreign = instance(new TestListener(), "foreign");

        assertThrows(IllegalArgumentException.class, () -> listener.registerInstance(foreign));
        assertThrows(IllegalArgumentException.class, () -> listener.replaceInstances(List.of(foreign)));
        assertFalse(listener.hasInstancesListening());
    }

    @Test
    void reactivationRejectsAsyncWorkCapturedBeforeDormancy() {
        TestListener listener = new TestListener();
        ListenerInstance instance = instance(listener, "instance");
        listener.registerInstance(instance);
        long queuedRevision = listener.getActiveInstanceRevision();

        listener.unregisterInstance(instance);
        listener.registerInstance(instance);
        long reactivatedRevision = listener.getActiveInstanceRevision();

        assertFalse(listener.isActiveAtRevision(queuedRevision));
        assertTrue(listener.isActiveAtRevision(reactivatedRevision));
        assertNotEquals(queuedRevision, reactivatedRevision);
        assertEquals(2, listener.activationCount);
        assertEquals(1, listener.deactivationCount);
    }

    @Test
    void dispatchUsesStableSnapshotWhenInstancesAreRemovedConcurrently() throws Exception {
        TestListener listener = new TestListener();
        ListenerInstance first = instance(listener, "first");
        ListenerInstance second = instance(listener, "second");
        CountDownLatch dispatchStarted = new CountDownLatch(1);
        CountDownLatch continueDispatch = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        first.getActionScript().addExecutable(new BlockingExecutable(dispatchStarted, continueDispatch, executions));
        second.getActionScript().addExecutable(new BlockingExecutable(dispatchStarted, continueDispatch, executions));
        listener.replaceInstances(List.of(first, second));

        Thread dispatchThread = new Thread(listener::trigger);
        dispatchThread.start();
        assertTrue(dispatchStarted.await(2L, TimeUnit.SECONDS));
        listener.replaceInstances(List.of());
        continueDispatch.countDown();
        dispatchThread.join(2000L);

        assertFalse(dispatchThread.isAlive());
        assertEquals(2, executions.get());
        assertFalse(listener.hasInstancesListening());
    }

    private static ListenerInstance instance(TestListener listener, String identifier) {
        ListenerInstance instance = new ListenerInstance(listener);
        instance.instanceIdentifier = identifier;
        return instance;
    }

    private static final class TestListener extends AbstractListener {

        private int activationCount;
        private int deactivationCount;

        private TestListener() {
            super("test");
        }

        private void trigger() {
            this.notifyAllInstances();
        }

        @Override
        protected void onActivated() {
            this.activationCount++;
        }

        @Override
        protected void onDeactivated() {
            this.deactivationCount++;
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.literal("Test");
        }

        @Override
        public @NotNull List<Component> getDescription() {
            return List.of();
        }
    }

    private record BlockingExecutable(CountDownLatch dispatchStarted, CountDownLatch continueDispatch, AtomicInteger executions) implements Executable {

        @Override
        public void execute() {
            this.dispatchStarted.countDown();
            try {
                if (!this.continueDispatch.await(2L, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to continue listener dispatch");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
            this.executions.incrementAndGet();
        }

        @Override
        public @NotNull String getIdentifier() {
            return "blocking";
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return this;
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test");
        }
    }
}
