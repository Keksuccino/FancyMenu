package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractListenerTest {

    @Test
    void lifecycleHooksOnlyRunForActiveStateTransitions() {
        TestListener listener = new TestListener("test_listener");
        ListenerInstance first = new ListenerInstance(listener);
        ListenerInstance second = new ListenerInstance(listener);

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());

        listener.registerInstance(first);
        long firstRevision = listener.getActiveInstanceRevision();

        assertTrue(listener.hasInstancesListening());
        assertTrue(listener.isActiveAtRevision(firstRevision));
        assertEquals(1, listener.activationCount);
        assertEquals(0, listener.deactivationCount);

        listener.registerInstance(first);

        assertEquals(firstRevision, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activationCount);

        listener.registerInstance(second);
        long secondRevision = listener.getActiveInstanceRevision();

        assertNotEquals(firstRevision, secondRevision);
        assertFalse(listener.isActiveAtRevision(firstRevision));
        assertEquals(1, listener.activationCount);

        listener.unregisterInstance(first);

        assertTrue(listener.hasInstancesListening());
        assertEquals(0, listener.deactivationCount);

        listener.unregisterInstance(second);

        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());
        assertEquals(1, listener.deactivationCount);
    }

    @Test
    void replacingAnIdentifierMakesStaleIdentityRemovalHarmless() {
        TestListener listener = new TestListener("test_listener");
        ListenerInstance original = new ListenerInstance(listener);
        ListenerInstance replacement = new ListenerInstance(listener);
        replacement.instanceIdentifier = original.instanceIdentifier;

        listener.registerInstance(original);
        listener.registerInstance(replacement);
        long replacementRevision = listener.getActiveInstanceRevision();
        listener.unregisterInstance(original);

        assertTrue(listener.hasInstancesListening());
        assertEquals(replacementRevision, listener.getActiveInstanceRevision());
        assertEquals(1, listener.activationCount);
        assertEquals(0, listener.deactivationCount);
    }

    @Test
    void bulkReplacementPublishesOneRevisionAndValidatesParentsBeforeMutation() {
        TestListener listener = new TestListener("test_listener");
        TestListener otherListener = new TestListener("other_listener");
        ListenerInstance first = new ListenerInstance(listener);
        ListenerInstance replacement = new ListenerInstance(listener);
        ListenerInstance wrongParent = new ListenerInstance(otherListener);

        listener.replaceInstances(List.of(first));
        long firstRevision = listener.getActiveInstanceRevision();
        listener.replaceInstances(List.of(first));

        assertEquals(firstRevision, listener.getActiveInstanceRevision());

        listener.replaceInstances(List.of(replacement));
        long replacementRevision = listener.getActiveInstanceRevision();

        assertNotEquals(firstRevision, replacementRevision);
        assertEquals(1, listener.activationCount);
        assertEquals(0, listener.deactivationCount);

        assertThrows(IllegalArgumentException.class, () -> listener.replaceInstances(List.of(wrongParent)));
        assertEquals(replacementRevision, listener.getActiveInstanceRevision());
        assertTrue(listener.hasInstancesListening());
    }

    @Test
    void dispatchUsesAStableSnapshotWhenAnActionUnregistersItself() {
        TestListener listener = new TestListener("test_listener");
        ListenerInstance selfRemoving = new ListenerInstance(listener);
        ListenerInstance remaining = new ListenerInstance(listener);
        AtomicInteger selfRemovingExecutions = new AtomicInteger();
        AtomicInteger remainingExecutions = new AtomicInteger();

        selfRemoving.setActionScript(new CallbackExecutableBlock(() -> {
            selfRemovingExecutions.incrementAndGet();
            listener.unregisterInstance(selfRemoving);
        }));
        remaining.setActionScript(new CallbackExecutableBlock(remainingExecutions::incrementAndGet));
        listener.replaceInstances(List.of(selfRemoving, remaining));

        listener.dispatch();

        assertEquals(1, selfRemovingExecutions.get());
        assertEquals(1, remainingExecutions.get());
        assertTrue(listener.hasInstancesListening());

        listener.dispatch();

        assertEquals(1, selfRemovingExecutions.get());
        assertEquals(2, remainingExecutions.get());
    }

    @Test
    void registeringAnInstanceWithTheWrongProviderIsRejected() {
        TestListener listener = new TestListener("test_listener");
        TestListener otherListener = new TestListener("other_listener");
        ListenerInstance wrongParent = new ListenerInstance(otherListener);

        assertThrows(IllegalArgumentException.class, () -> listener.registerInstance(wrongParent));
        assertFalse(listener.hasInstancesListening());
        assertEquals(-1L, listener.getActiveInstanceRevision());
    }

    private static final class TestListener extends AbstractListener {

        private int activationCount;
        private int deactivationCount;

        private TestListener(String identifier) {
            super(identifier);
        }

        private void dispatch() {
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
        public Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public List<Component> getDescription() {
            return List.of();
        }

    }

    private static final class CallbackExecutableBlock extends GenericExecutableBlock {

        private final Runnable callback;

        private CallbackExecutableBlock(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void execute() {
            this.callback.run();
        }

    }

}
