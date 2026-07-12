package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractListenerTest {

    @Test
    void lifecycleAndRevisionFollowRealRegistryChanges() {
        TestListener listener = new TestListener("lifecycle_test");
        ListenerInstance first = listener.createFreshInstance();
        ListenerInstance second = listener.createFreshInstance();

        assertEquals(-1L, listener.getActiveInstanceRevision());
        listener.registerInstance(first);
        long firstRevision = listener.getActiveInstanceRevision();
        assertTrue(firstRevision >= 0L);
        assertEquals(1, listener.activations);
        assertEquals(0, listener.deactivations);

        listener.registerInstance(first);
        assertEquals(firstRevision, listener.getActiveInstanceRevision());

        listener.registerInstance(second);
        long secondRevision = listener.getActiveInstanceRevision();
        assertTrue(secondRevision > firstRevision);
        assertEquals(1, listener.activations);

        listener.unregisterInstance("missing");
        assertEquals(secondRevision, listener.getActiveInstanceRevision());
        listener.unregisterInstance(first);
        assertTrue(listener.hasInstancesListening());
        listener.unregisterInstance(second);

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activations);
        assertEquals(1, listener.deactivations);
    }

    @Test
    void replacementInvalidatesCapturedRevisionWithoutExtraLifecycleTransitions() {
        TestListener listener = new TestListener("revision_test");
        ListenerInstance original = listener.createFreshInstance();
        original.instanceIdentifier = "shared";
        listener.replaceInstances(List.of(original));
        long capturedRevision = listener.getActiveInstanceRevision();

        listener.replaceInstances(List.of(original));
        assertTrue(listener.isActiveAtRevision(capturedRevision));

        ListenerInstance replacement = listener.createFreshInstance();
        replacement.instanceIdentifier = "shared";
        listener.replaceInstances(List.of(replacement));

        assertFalse(listener.isActiveAtRevision(capturedRevision));
        assertTrue(listener.isActiveAtRevision(listener.getActiveInstanceRevision()));
        assertEquals(1, listener.activations);
        assertEquals(0, listener.deactivations);

        listener.replaceInstances(List.of());
        assertEquals(1, listener.deactivations);
    }

    @Test
    void dispatchUsesSnapshotWhenInstancesRemoveEachOther() {
        TestListener listener = new TestListener("snapshot_test");
        ListenerInstance first = listener.createFreshInstance();
        ListenerInstance second = listener.createFreshInstance();
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        first.getActionScript().addExecutable(new RunnableExecutable(() -> {
            firstCalls.incrementAndGet();
            listener.unregisterInstance(second);
        }));
        second.getActionScript().addExecutable(new RunnableExecutable(() -> {
            secondCalls.incrementAndGet();
            listener.unregisterInstance(first);
        }));
        listener.replaceInstances(List.of(first, second));

        listener.fire();

        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
        assertFalse(listener.hasInstancesListening());
        assertEquals(1, listener.deactivations);
    }

    @Test
    void wrongParentInstancesAreRejectedAtomically() {
        TestListener target = new TestListener("target_test");
        TestListener other = new TestListener("other_test");
        ListenerInstance wrong = other.createFreshInstance();

        assertThrows(IllegalArgumentException.class, () -> target.registerInstance(wrong));
        assertThrows(IllegalArgumentException.class, () -> target.replaceInstances(List.of(target.createFreshInstance(), wrong)));
        assertFalse(target.hasInstancesListening());
        assertEquals(0, target.activations);
    }

    private static final class TestListener extends AbstractListener {

        private int activations;
        private int deactivations;

        private TestListener(String identifier) {
            super(identifier);
        }

        private void fire() {
            this.notifyAllInstances();
        }

        @Override
        protected void onActivated() {
            this.activations++;
        }

        @Override
        protected void onDeactivated() {
            this.deactivations++;
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.literal(this.identifier);
        }

        @Override
        public @NotNull List<Component> getDescription() {
            return List.of();
        }
    }

    private static final class RunnableExecutable implements Executable {

        private final String identifier = UUID.randomUUID().toString();
        private final Runnable runnable;

        private RunnableExecutable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void execute() {
            this.runnable.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new RunnableExecutable(this.runnable);
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test_executable");
        }
    }
}
