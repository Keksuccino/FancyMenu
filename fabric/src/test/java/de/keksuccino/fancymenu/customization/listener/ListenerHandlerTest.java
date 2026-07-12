package de.keksuccino.fancymenu.customization.listener;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerHandlerTest {

    private static final TestListener FIRST_PROVIDER = new TestListener("handler_test_first");
    private static final TestListener SECOND_PROVIDER = new TestListener("handler_test_second");

    @BeforeAll
    static void registerTestProviders() {
        ListenerRegistry.register(FIRST_PROVIDER);
        ListenerRegistry.register(SECOND_PROVIDER);
    }

    @BeforeEach
    void resetProviderCounters() throws Exception {
        replaceInstancesInternally(List.of());
        FIRST_PROVIDER.activations = 0;
        FIRST_PROVIDER.deactivations = 0;
        SECOND_PROVIDER.activations = 0;
        SECOND_PROVIDER.deactivations = 0;
    }

    @AfterEach
    void clearHandlerState() throws Exception {
        replaceInstancesInternally(List.of());
    }

    @Test
    void sameIdentifierMigratesAtomicallyBetweenProviders() throws Exception {
        ListenerInstance first = FIRST_PROVIDER.createFreshInstance();
        first.instanceIdentifier = "shared-handler-id";
        replaceInstancesInternally(List.of(first));
        assertTrue(FIRST_PROVIDER.hasInstancesListening());
        assertFalse(SECOND_PROVIDER.hasInstancesListening());

        ListenerInstance second = SECOND_PROVIDER.createFreshInstance();
        second.instanceIdentifier = "shared-handler-id";
        replaceInstancesInternally(List.of(first, second));

        assertFalse(FIRST_PROVIDER.hasInstancesListening());
        assertTrue(SECOND_PROVIDER.hasInstancesListening());
        assertSame(second, handlerInstances().get("shared-handler-id"));
        assertEquals(1, FIRST_PROVIDER.activations);
        assertEquals(1, FIRST_PROVIDER.deactivations);
        assertEquals(1, SECOND_PROVIDER.activations);
        assertEquals(0, SECOND_PROVIDER.deactivations);
    }

    @Test
    void bulkReplacementUpdatesEveryProviderAsOneRegistryState() throws Exception {
        ListenerInstance first = FIRST_PROVIDER.createFreshInstance();
        first.instanceIdentifier = "first-id";
        ListenerInstance second = SECOND_PROVIDER.createFreshInstance();
        second.instanceIdentifier = "second-id";
        replaceInstancesInternally(List.of(first, second));
        int firstActivationCount = FIRST_PROVIDER.activations;

        ListenerInstance replacement = FIRST_PROVIDER.createFreshInstance();
        replacement.instanceIdentifier = "replacement-id";
        replaceInstancesInternally(List.of(replacement));

        assertTrue(FIRST_PROVIDER.hasInstancesListening());
        assertFalse(SECOND_PROVIDER.hasInstancesListening());
        assertEquals(firstActivationCount, FIRST_PROVIDER.activations);
        assertEquals(1, handlerInstances().size());
        assertSame(replacement, handlerInstances().get("replacement-id"));
    }

    private static void replaceInstancesInternally(Collection<ListenerInstance> replacements) throws Exception {
        Method method = ListenerHandler.class.getDeclaredMethod("replaceInstancesInternal", Collection.class);
        method.setAccessible(true);
        method.invoke(null, replacements);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ListenerInstance> handlerInstances() throws Exception {
        Field field = ListenerHandler.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        return (Map<String, ListenerInstance>)field.get(null);
    }

    private static final class TestListener extends AbstractListener {

        private int activations;
        private int deactivations;

        private TestListener(String identifier) {
            super(identifier);
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
}
