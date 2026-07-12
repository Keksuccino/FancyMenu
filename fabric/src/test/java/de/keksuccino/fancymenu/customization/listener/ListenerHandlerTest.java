package de.keksuccino.fancymenu.customization.listener;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerHandlerTest {

    private static final AtomicInteger NEXT_PROVIDER_ID = new AtomicInteger();

    @AfterEach
    void clearInstances() {
        ListenerHandler.replaceInstancesInternal(List.of());
    }

    @Test
    void bulkReplacementMigratesSameIdentifierBetweenProviders() {
        TestListener firstProvider = registerProvider("migration_first");
        TestListener secondProvider = registerProvider("migration_second");
        ListenerInstance original = createInstance(firstProvider, "shared_instance");
        ListenerInstance replacement = createInstance(secondProvider, "shared_instance");

        ListenerHandler.replaceInstancesInternal(List.of(original));
        ListenerHandler.replaceInstancesInternal(List.of(replacement));

        assertFalse(firstProvider.hasInstancesListening());
        assertTrue(secondProvider.hasInstancesListening());
        assertEquals(1, firstProvider.activations.get());
        assertEquals(1, firstProvider.deactivations.get());
        assertEquals(1, secondProvider.activations.get());
        assertEquals(0, secondProvider.deactivations.get());
        List<ListenerInstance> published = ListenerHandler.getInstancesSnapshot();
        assertEquals(1, published.size());
        assertSame(replacement, published.get(0));
    }

    @Test
    void duplicateIdentifiersUseTheLastReplacementWithoutLeavingTheOldProviderActive() {
        TestListener firstProvider = registerProvider("duplicate_first");
        TestListener secondProvider = registerProvider("duplicate_second");
        ListenerInstance discarded = createInstance(firstProvider, "duplicate_instance");
        ListenerInstance retained = createInstance(secondProvider, "duplicate_instance");

        ListenerHandler.replaceInstancesInternal(List.of(discarded, retained));

        assertFalse(firstProvider.hasInstancesListening());
        assertTrue(secondProvider.hasInstancesListening());
        assertEquals(0, firstProvider.activations.get());
        assertEquals(List.of(retained), ListenerHandler.getInstancesSnapshot());
    }

    @Test
    void globalSnapshotsObserveOnlyCompleteBulkPublications() throws Exception {
        TestListener firstProvider = registerProvider("atomic_first");
        TestListener secondProvider = registerProvider("atomic_second");
        List<ListenerInstance> firstSet = List.of(createInstance(firstProvider, "atomic_a"), createInstance(secondProvider, "atomic_b"));
        List<ListenerInstance> secondSet = List.of(createInstance(firstProvider, "atomic_c"), createInstance(firstProvider, "atomic_d"), createInstance(secondProvider, "atomic_e"));
        Set<String> firstIdentifiers = identifiers(firstSet);
        Set<String> secondIdentifiers = identifiers(secondSet);
        ListenerHandler.replaceInstancesInternal(firstSet);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> writer = executor.submit(() -> {
            start.await();
            for (int iteration = 0; iteration < 500; iteration++) {
                ListenerHandler.replaceInstancesInternal((iteration & 1) == 0 ? secondSet : firstSet);
            }
            return null;
        });
        Future<?> reader = executor.submit(() -> {
            start.await();
            for (int iteration = 0; iteration < 2000; iteration++) {
                Set<String> snapshot = identifiers(ListenerHandler.getInstancesSnapshot());
                assertTrue(snapshot.equals(firstIdentifiers) || snapshot.equals(secondIdentifiers), snapshot.toString());
            }
            return null;
        });

        try {
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static TestListener registerProvider(String name) {
        TestListener provider = new TestListener("test_" + name + "_" + NEXT_PROVIDER_ID.incrementAndGet());
        ListenerRegistry.register(provider);
        return provider;
    }

    private static ListenerInstance createInstance(AbstractListener provider, String identifier) {
        ListenerInstance instance = provider.createFreshInstance();
        instance.instanceIdentifier = identifier;
        return instance;
    }

    private static Set<String> identifiers(List<ListenerInstance> instances) {
        List<String> identifiers = new ArrayList<>();
        for (ListenerInstance instance : instances) {
            identifiers.add(instance.instanceIdentifier);
        }
        return Set.copyOf(identifiers);
    }

    private static final class TestListener extends AbstractListener {

        private final AtomicInteger activations = new AtomicInteger();
        private final AtomicInteger deactivations = new AtomicInteger();

        private TestListener(String identifier) {
            super(identifier);
        }

        @Override
        protected void onActivated() {
            this.activations.incrementAndGet();
        }

        @Override
        protected void onDeactivated() {
            this.deactivations.incrementAndGet();
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(this.getIdentifier());
        }

        @Override
        public List<Component> getDescription() {
            return List.of();
        }

    }

}
