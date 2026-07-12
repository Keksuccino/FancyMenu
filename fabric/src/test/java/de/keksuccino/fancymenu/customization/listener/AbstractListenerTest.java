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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractListenerTest {

    @Test
    void registrationPublishesRevisionsAndRunsLifecycleHooksOnlyAtActivityBoundaries() {
        TestListener listener = new TestListener("test");
        ListenerInstance first = createInstance(listener, "first", () -> {});
        ListenerInstance second = createInstance(listener, "second", () -> {});

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());

        listener.registerInstance(first);
        long firstRevision = listener.getActiveInstanceRevision();

        assertTrue(firstRevision >= 0L);
        assertTrue(listener.isActiveAtRevision(firstRevision));
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());
        assertTrue(listener.activeDuringActivation);

        listener.registerInstance(second);
        long secondRevision = listener.getActiveInstanceRevision();

        assertTrue(secondRevision > firstRevision);
        assertFalse(listener.isActiveAtRevision(firstRevision));
        assertEquals(1, listener.activations.get());

        listener.unregisterInstance(first);
        long thirdRevision = listener.getActiveInstanceRevision();

        assertTrue(thirdRevision > secondRevision);
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());

        listener.unregisterInstance(second);

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());
        assertFalse(listener.isActiveAtRevision(thirdRevision));
        assertEquals(1, listener.activations.get());
        assertEquals(1, listener.deactivations.get());
        assertFalse(listener.activeDuringDeactivation);
    }

    @Test
    void registeringTheSameObjectAndRemovingMissingInstancesAreNoOps() {
        TestListener listener = new TestListener("test");
        ListenerInstance instance = createInstance(listener, "instance", () -> {});

        listener.registerInstance(instance);
        long revision = listener.getActiveInstanceRevision();
        listener.registerInstance(instance);
        listener.unregisterInstance("missing");
        listener.unregisterInstance(createInstance(listener, "instance", () -> {}));

        assertEquals(revision, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());
    }

    @Test
    void replacingAnIdentifierWithANewInstanceInvalidatesTheRevisionWithoutTogglingLifecycle() {
        TestListener listener = new TestListener("test");
        AtomicInteger firstExecutions = new AtomicInteger();
        AtomicInteger replacementExecutions = new AtomicInteger();
        ListenerInstance first = createInstance(listener, "shared", firstExecutions::incrementAndGet);
        ListenerInstance replacement = createInstance(listener, "shared", replacementExecutions::incrementAndGet);

        listener.registerInstance(first);
        long firstRevision = listener.getActiveInstanceRevision();
        listener.registerInstance(replacement);

        assertFalse(listener.isActiveAtRevision(firstRevision));
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());

        listener.fire();

        assertEquals(0, firstExecutions.get());
        assertEquals(1, replacementExecutions.get());
    }

    @Test
    void atomicReplacementTreatsAnIdenticalInstanceMapAsANoOp() {
        TestListener listener = new TestListener("test");
        ListenerInstance first = createInstance(listener, "first", () -> {});
        ListenerInstance second = createInstance(listener, "second", () -> {});

        listener.replaceInstances(List.of(first, second));
        long initialRevision = listener.getActiveInstanceRevision();
        listener.replaceInstances(List.of(second, first));

        assertEquals(initialRevision, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());

        listener.replaceInstances(List.of(first));
        long reducedRevision = listener.getActiveInstanceRevision();

        assertTrue(reducedRevision > initialRevision);
        assertEquals(1, listener.activations.get());
        assertEquals(0, listener.deactivations.get());

        listener.replaceInstances(List.of());
        listener.replaceInstances(List.of());

        assertFalse(listener.hasInstancesListening());
        assertEquals(1, listener.activations.get());
        assertEquals(1, listener.deactivations.get());
    }

    @Test
    void duplicateIdentifiersInAReplacementUseTheLastInstance() {
        TestListener listener = new TestListener("test");
        AtomicInteger firstExecutions = new AtomicInteger();
        AtomicInteger lastExecutions = new AtomicInteger();
        ListenerInstance first = createInstance(listener, "duplicate", firstExecutions::incrementAndGet);
        ListenerInstance last = createInstance(listener, "duplicate", lastExecutions::incrementAndGet);

        listener.replaceInstances(List.of(first, last));
        listener.fire();

        assertEquals(0, firstExecutions.get());
        assertEquals(1, lastExecutions.get());
    }

    @Test
    void wrongParentRegistrationAndReplacementLeaveExistingStateUntouched() {
        TestListener listener = new TestListener("test");
        TestListener other = new TestListener("other");
        AtomicInteger executions = new AtomicInteger();
        ListenerInstance existing = createInstance(listener, "existing", executions::incrementAndGet);
        ListenerInstance wrongParent = createInstance(other, "foreign", () -> {});

        listener.registerInstance(existing);
        long revision = listener.getActiveInstanceRevision();

        assertThrows(IllegalArgumentException.class, () -> listener.registerInstance(wrongParent));
        assertThrows(IllegalArgumentException.class, () -> listener.replaceInstances(List.of(existing, wrongParent)));
        assertEquals(revision, listener.getActiveInstanceRevision());
        assertTrue(listener.isActiveAtRevision(revision));

        listener.fire();

        assertEquals(1, executions.get());
        assertFalse(other.hasInstancesListening());
    }

    @Test
    void dispatchUsesAStableSnapshotWhenAnActionUnregistersItself() {
        TestListener listener = new TestListener("test");
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<ListenerInstance> instanceReference = new AtomicReference<>();
        ListenerInstance instance = createInstance(listener, "self-removing", () -> {
            executions.incrementAndGet();
            listener.unregisterInstance(instanceReference.get());
        });
        instanceReference.set(instance);
        listener.registerInstance(instance);

        assertDoesNotThrow(listener::fire);

        assertEquals(1, executions.get());
        assertFalse(listener.hasInstancesListening());
        assertEquals(1, listener.deactivations.get());
    }

    @Test
    void concurrentReplacementDoesNotChangeAnInProgressDispatchSnapshot() throws Exception {
        TestListener listener = new TestListener("test");
        CountDownLatch oldActionStarted = new CountDownLatch(1);
        CountDownLatch releaseOldAction = new CountDownLatch(1);
        AtomicInteger oldExecutions = new AtomicInteger();
        AtomicInteger replacementExecutions = new AtomicInteger();
        ListenerInstance oldInstance = createInstance(listener, "old", () -> {
            oldExecutions.incrementAndGet();
            oldActionStarted.countDown();
            await(releaseOldAction);
        });
        ListenerInstance replacement = createInstance(listener, "replacement", replacementExecutions::incrementAndGet);
        listener.registerInstance(oldInstance);
        Thread dispatchThread = new Thread(listener::fire, "listener-snapshot-test");

        dispatchThread.start();
        try {
            assertTrue(oldActionStarted.await(5L, TimeUnit.SECONDS));
            listener.replaceInstances(List.of(replacement));
        } finally {
            releaseOldAction.countDown();
        }
        dispatchThread.join(5000L);

        assertFalse(dispatchThread.isAlive());
        assertEquals(1, oldExecutions.get());
        assertEquals(0, replacementExecutions.get());

        listener.fire();

        assertEquals(1, replacementExecutions.get());
    }

    private static ListenerInstance createInstance(TestListener parent, String identifier, Runnable action) {
        ListenerInstance instance = new ListenerInstance(parent);
        instance.instanceIdentifier = identifier;
        instance.getActionScript().addExecutable(new TestExecutable(identifier + "-action", action));
        return instance;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for listener lifecycle test coordination");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for listener lifecycle test coordination", ex);
        }
    }

    private static final class TestListener extends AbstractListener {

        private final AtomicInteger activations = new AtomicInteger();
        private final AtomicInteger deactivations = new AtomicInteger();
        private boolean activeDuringActivation;
        private boolean activeDuringDeactivation;

        private TestListener(String identifier) {
            super(identifier);
        }

        private void fire() {
            this.notifyAllInstances();
        }

        @Override
        protected void onActivated() {
            this.activeDuringActivation = this.hasInstancesListening();
            this.activations.incrementAndGet();
        }

        @Override
        protected void onDeactivated() {
            this.activeDuringDeactivation = this.hasInstancesListening();
            this.deactivations.incrementAndGet();
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

    private record TestExecutable(String identifier, Runnable action) implements Executable {

        @Override
        public void execute() {
            this.action.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
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
