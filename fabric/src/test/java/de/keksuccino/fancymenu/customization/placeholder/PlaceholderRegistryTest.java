package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry is global and has no removal API, so every test registers placeholders with its own identifiers.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderRegistryTest {

    @Test
    void nullIdentifierFindsNothing() {
        registerOnce(new TestPlaceholder("registry_test_null_lookup", List.of("registry_test_null_lookup_alias"), null));
        assertNull(PlaceholderRegistry.getPlaceholder(null));
    }

    @Test
    void placeholderWithEmptyIdentifierHasNoPlaceholderAndNoValues() {
        registerOnce(new TestPlaceholder("registry_test_empty_identifier", List.of("registry_test_empty_identifier_alias"), null));
        String text = "{\"placeholder\":\"\"}";
        List<PlaceholderParser.ParsedPlaceholder> found = PlaceholderParser.findPlaceholders(text, new HashMap<>());
        assertEquals(1, found.size());
        PlaceholderParser.ParsedPlaceholder parsed = found.get(0);
        assertNull(parsed.getIdentifier());
        assertNull(parsed.getPlaceholder());
        assertFalse(parsed.hasValues());
        assertNull(parsed.getValues());
        assertEquals(text, parsed.getReplacement());
    }

    @Test
    void mainIdentifierIsFoundDirectly() {
        TestPlaceholder placeholder = new TestPlaceholder("registry_test_main", null, null);
        PlaceholderRegistry.register(placeholder);
        assertSame(placeholder, PlaceholderRegistry.getPlaceholder("registry_test_main"));
        assertNull(PlaceholderRegistry.getPlaceholder("registry_test_main_unknown"));
    }

    @Test
    void alternativeIdentifierFindsItsPlaceholder() {
        TestPlaceholder placeholder = new TestPlaceholder("registry_test_alternative", List.of("registry_test_alternative_a", "registry_test_alternative_b"), null);
        PlaceholderRegistry.register(placeholder);
        assertSame(placeholder, PlaceholderRegistry.getPlaceholder("registry_test_alternative_a"));
        assertSame(placeholder, PlaceholderRegistry.getPlaceholder("registry_test_alternative_b"));
    }

    @Test
    void mainIdentifierWinsOverAlternativeIdentifierOfEarlierPlaceholder() {
        TestPlaceholder withAlternative = new TestPlaceholder("registry_test_early_alias_owner", List.of("registry_test_contested_late_main"), null);
        TestPlaceholder withMain = new TestPlaceholder("registry_test_contested_late_main", null, null);
        PlaceholderRegistry.register(withAlternative);
        assertSame(withAlternative, PlaceholderRegistry.getPlaceholder("registry_test_contested_late_main"));
        PlaceholderRegistry.register(withMain);
        assertSame(withMain, PlaceholderRegistry.getPlaceholder("registry_test_contested_late_main"));
        assertSame(withAlternative, PlaceholderRegistry.getPlaceholder("registry_test_early_alias_owner"));
    }

    @Test
    void mainIdentifierWinsOverAlternativeIdentifierOfLaterPlaceholder() {
        TestPlaceholder withMain = new TestPlaceholder("registry_test_contested_early_main", null, null);
        TestPlaceholder withAlternative = new TestPlaceholder("registry_test_late_alias_owner", List.of("registry_test_contested_early_main"), null);
        PlaceholderRegistry.register(withMain);
        PlaceholderRegistry.register(withAlternative);
        assertSame(withMain, PlaceholderRegistry.getPlaceholder("registry_test_contested_early_main"));
    }

    @Test
    void firstRegisteredAlternativeIdentifierWins() {
        TestPlaceholder first = new TestPlaceholder("registry_test_first_alias_owner", List.of("registry_test_shared_alias"), null);
        TestPlaceholder second = new TestPlaceholder("registry_test_second_alias_owner", List.of("registry_test_shared_alias"), null);
        PlaceholderRegistry.register(first);
        PlaceholderRegistry.register(second);
        assertSame(first, PlaceholderRegistry.getPlaceholder("registry_test_shared_alias"));
    }

    @Test
    void duplicateMainIdentifierIsRejectedAndKeepsTheFirstPlaceholder() {
        TestPlaceholder first = new TestPlaceholder("registry_test_duplicate", null, null);
        PlaceholderRegistry.register(first);
        long revision = PlaceholderRegistry.getRevision();
        assertThrows(IllegalStateException.class, () -> PlaceholderRegistry.register(new TestPlaceholder("registry_test_duplicate", null, null)));
        assertSame(first, PlaceholderRegistry.getPlaceholder("registry_test_duplicate"));
        assertEquals(revision, PlaceholderRegistry.getRevision());
    }

    @Test
    void registrationKeepsTheOrderAndCallsOnRegistered() {
        TestPlaceholder first = new TestPlaceholder("registry_test_order_first", null, null);
        TestPlaceholder second = new TestPlaceholder("registry_test_order_second", null, null);
        PlaceholderRegistry.register(first);
        PlaceholderRegistry.register(second);
        List<Placeholder> placeholders = PlaceholderRegistry.getPlaceholders();
        assertTrue(placeholders.indexOf(first) < placeholders.indexOf(second));
        assertEquals(1, first.registrations.get());
        assertEquals(1, second.registrations.get());
        placeholders.clear();
        assertTrue(PlaceholderRegistry.getPlaceholders().contains(first));
    }

    @Test
    void revisionIncreasesWithEveryRegistration() {
        long before = PlaceholderRegistry.getRevision();
        PlaceholderRegistry.register(new TestPlaceholder("registry_test_revision_a", null, null));
        long afterFirst = PlaceholderRegistry.getRevision();
        PlaceholderRegistry.register(new TestPlaceholder("registry_test_revision_b", List.of("registry_test_revision_b_alias"), null));
        long afterSecond = PlaceholderRegistry.getRevision();
        assertTrue(afterFirst > before);
        assertTrue(afterSecond > afterFirst);
    }

    @Test
    void valueNamesGetReadOnceWhenThePlaceholderGetsRegistered() {
        AtomicInteger valueNameCalls = new AtomicInteger();
        TestPlaceholder placeholder = new TestPlaceholder("registry_test_value_names", null, () -> {
            valueNameCalls.incrementAndGet();
            return new ArrayList<>(List.of("first", "second"));
        });
        PlaceholderRegistry.register(placeholder);
        assertEquals(1, valueNameCalls.get());
        PlaceholderParser.PlaceholderCachingController original = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        try {
            String text = "{\"placeholder\":\"registry_test_value_names\",\"values\":{\"first\":\"a\",\"second\":\"b\"}}";
            for (int i = 0; i < 3; i++) assertEquals("a|b", PlaceholderParser.replacePlaceholders(text));
            PlaceholderParser.ParsedPlaceholder parsed = PlaceholderParser.findPlaceholders(text, new HashMap<>()).get(0);
            assertTrue(parsed.hasValues());
            assertEquals(2, parsed.getValues().size());
        } finally {
            PlaceholderParser.setPlaceholderCachingController(original);
        }
        assertEquals(1, valueNameCalls.get());
        assertEquals(List.of("first", "second"), PlaceholderRegistry.getValueNames(placeholder));
    }

    @Test
    void valueNamesOfUnregisteredPlaceholderComeFromThePlaceholder() {
        TestPlaceholder unregistered = new TestPlaceholder("registry_test_unregistered", null, () -> List.of("text"));
        assertEquals(List.of("text"), PlaceholderRegistry.getValueNames(unregistered));
        TestPlaceholder withoutValues = new TestPlaceholder("registry_test_unregistered_without_values", null, () -> null);
        assertEquals(List.of(), PlaceholderRegistry.getValueNames(withoutValues));
    }

    @Test
    void placeholderWithSameIdentifierAsRegisteredOneDoesNotGetItsValueNames() {
        TestPlaceholder registered = new TestPlaceholder("registry_test_same_identifier", null, () -> List.of("registered"));
        PlaceholderRegistry.register(registered);
        TestPlaceholder other = new TestPlaceholder("registry_test_same_identifier", null, () -> List.of("other"));
        assertEquals(List.of("other"), PlaceholderRegistry.getValueNames(other));
    }

    @Test
    void valueNamesThatFailDuringRegistrationGetReadAgainLater() {
        AtomicInteger valueNameCalls = new AtomicInteger();
        TestPlaceholder placeholder = new TestPlaceholder("registry_test_failing_value_names", null, () -> {
            if (valueNameCalls.getAndIncrement() == 0) throw new IllegalStateException("not ready yet");
            return List.of("text");
        });
        PlaceholderRegistry.register(placeholder);
        assertSame(placeholder, PlaceholderRegistry.getPlaceholder("registry_test_failing_value_names"));
        assertEquals(List.of("text"), PlaceholderRegistry.getValueNames(placeholder));
    }

    @Test
    void concurrentRegistrationsAreAllVisible() throws Exception {
        int threads = 8;
        int perThread = 25;
        long before = PlaceholderRegistry.getRevision();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int thread = 0; thread < threads; thread++) {
                int threadIndex = thread;
                futures.add(executor.submit(() -> {
                    await(start);
                    for (int i = 0; i < perThread; i++) {
                        PlaceholderRegistry.register(new TestPlaceholder("registry_test_concurrent_" + threadIndex + "_" + i, List.of("registry_test_concurrent_alias_" + threadIndex + "_" + i), null));
                    }
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(10L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
        assertEquals(before + (threads * perThread), PlaceholderRegistry.getRevision());
        Set<String> registered = PlaceholderRegistry.getPlaceholders().stream().map(Placeholder::getIdentifier).collect(Collectors.toSet());
        for (int thread = 0; thread < threads; thread++) {
            for (int i = 0; i < perThread; i++) {
                String identifier = "registry_test_concurrent_" + thread + "_" + i;
                assertTrue(registered.contains(identifier));
                Placeholder placeholder = PlaceholderRegistry.getPlaceholder(identifier);
                assertSame(placeholder, PlaceholderRegistry.getPlaceholder("registry_test_concurrent_alias_" + thread + "_" + i));
            }
        }
    }

    private static void registerOnce(@NotNull Placeholder placeholder) {
        if (PlaceholderRegistry.getPlaceholder(placeholder.getIdentifier()) == null) PlaceholderRegistry.register(placeholder);
    }

    private static void await(@NotNull CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for the start signal");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static final class TestPlaceholder extends Placeholder {

        private final List<String> alternativeIdentifiers;
        private final Supplier<List<String>> valueNames;
        private final AtomicInteger registrations = new AtomicInteger();

        private TestPlaceholder(@NotNull String identifier, @Nullable List<String> alternativeIdentifiers, @Nullable Supplier<List<String>> valueNames) {
            super(identifier);
            this.alternativeIdentifiers = alternativeIdentifiers;
            this.valueNames = valueNames;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            return dps.values.get("first") + "|" + dps.values.get("second");
        }

        @Override
        public List<String> getValueNames() {
            return (this.valueNames != null) ? this.valueNames.get() : null;
        }

        @Override
        public List<String> getAlternativeIdentifiers() {
            return this.alternativeIdentifiers;
        }

        @Override
        public void onRegistered() {
            this.registrations.incrementAndGet();
        }

        @Override
        public @NotNull String getDisplayName() {
            return this.getIdentifier();
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
            return new DeserializedPlaceholderString(this.getIdentifier(), new HashMap<>(), "");
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
