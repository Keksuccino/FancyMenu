package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class PlaceholderParserConcurrencyTest {

    private final List<Long> processorIds = new ArrayList<>();
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreGlobalState() {
        for (long processorId : this.processorIds) PlaceholderParser.removeParsingProcessor(processorId);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void registrationDuringIterationStartsWithTheNextParse() throws Exception {
        String source = "processor-snapshot-source";
        CountDownLatch firstProcessorEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstProcessor = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> {
            String processed = value + "|first";
            if (source.equals(value) && blockOnce.compareAndSet(true, false)) {
                firstProcessorEntered.countDown();
                await(releaseFirstProcessor);
            }
            return processed;
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> inFlight = executor.submit(() -> PlaceholderParser.replacePlaceholders(source));
        try {
            assertTrue(firstProcessorEntered.await(5L, TimeUnit.SECONDS));
            this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> value + "|second");
            releaseFirstProcessor.countDown();

            assertEquals(source + "|first", inFlight.get(5L, TimeUnit.SECONDS));
            assertEquals(source + "|first|second", PlaceholderParser.replacePlaceholders(source));
        } finally {
            releaseFirstProcessor.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentRegistrationsPublishEveryProcessorWithUniqueIds() throws Exception {
        int registrations = 64;
        String source = "concurrent-registration-source";
        AtomicInteger invocations = new AtomicInteger();
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int registration = 0; registration < registrations; registration++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    long id = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> {
                        if (source.equals(value)) invocations.incrementAndGet();
                        return value;
                    });
                    ids.add(id);
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(5L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
            this.processorIds.addAll(ids);
        }

        assertEquals(registrations, ids.size());
        assertEquals(source, PlaceholderParser.replacePlaceholders(source));
        assertEquals(registrations, invocations.get());
    }

    @Test
    void obsoleteInFlightParseCannotPublishAUsableCacheEntryAfterAddAndRemove() throws Exception {
        String placeholder = "{\"placeholder\":\"parser_revision_cache_test\"}";
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        CountDownLatch obsoleteProcessorEntered = new CountDownLatch(1);
        CountDownLatch releaseObsoleteProcessor = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        long obsoleteProcessorId = this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> {
            if (blockOnce.compareAndSet(true, false)) {
                obsoleteProcessorEntered.countDown();
                await(releaseObsoleteProcessor);
            }
            return value.replace(placeholder, "obsolete");
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> obsoleteParse = executor.submit(() -> PlaceholderParser.replacePlaceholders(placeholder));
        AtomicInteger currentProcessorInvocations = new AtomicInteger();
        try {
            assertTrue(obsoleteProcessorEntered.await(5L, TimeUnit.SECONDS));
            this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> {
                currentProcessorInvocations.incrementAndGet();
                return value.replace(placeholder, "current");
            });
            PlaceholderParser.removeParsingProcessor(obsoleteProcessorId);

            assertEquals("current", PlaceholderParser.replacePlaceholders(placeholder));
            releaseObsoleteProcessor.countDown();
            assertEquals("obsolete", obsoleteParse.get(5L, TimeUnit.SECONDS));

            assertEquals("current", PlaceholderParser.replacePlaceholders(placeholder));
            assertEquals(2, currentProcessorInvocations.get());
            assertEquals("current", PlaceholderParser.replacePlaceholders(placeholder));
            assertEquals(2, currentProcessorInvocations.get());
        } finally {
            releaseObsoleteProcessor.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

    @Test
    void changingCachingControllerInvalidatesPreviouslyCachedResults() {
        String placeholder = "{\"placeholder\":\"parser_controller_cache_test\"}";
        AtomicReference<String> replacement = new AtomicReference<>("first");
        AtomicInteger invocations = new AtomicInteger();
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> {
            invocations.incrementAndGet();
            return value.replace(placeholder, replacement.get());
        });
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));

        assertEquals("first", PlaceholderParser.replacePlaceholders(placeholder));
        replacement.set("second");
        assertEquals("first", PlaceholderParser.replacePlaceholders(placeholder));
        assertEquals(1, invocations.get());

        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        assertEquals("second", PlaceholderParser.replacePlaceholders(placeholder));
        assertEquals(2, invocations.get());
    }

    @Test
    void cacheUsesTheRawInputKeyOnLookupAndInsertion() {
        String alias = "raw-cache-alias";
        String transformedAlias = "{\"placeholder\":\"raw_cache_alias_result\"}";
        String transformedDirect = "{\"placeholder\":\"raw_cache_direct_result\"}";
        AtomicInteger beforeInvocations = new AtomicInteger();
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> {
            if (alias.equals(value)) {
                beforeInvocations.incrementAndGet();
                return transformedAlias;
            }
            if (transformedAlias.equals(value)) {
                beforeInvocations.incrementAndGet();
                return transformedDirect;
            }
            return value;
        });
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> value.replace(transformedAlias, "alias-result").replace(transformedDirect, "direct-result"));
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));

        assertEquals("alias-result", PlaceholderParser.replacePlaceholders(alias));
        assertEquals("direct-result", PlaceholderParser.replacePlaceholders(transformedAlias));
        assertEquals(2, beforeInvocations.get());

        assertEquals("alias-result", PlaceholderParser.replacePlaceholders(alias));
        assertEquals("direct-result", PlaceholderParser.replacePlaceholders(transformedAlias));
        assertEquals(2, beforeInvocations.get());
    }

    @Test
    void nestedReplacementRetainsTheSnapshotCapturedByItsParsedPlaceholder() {
        String nestedValue = "nested-snapshot-source";
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> value + "|first");
        NestedValueParsedPlaceholder parsedPlaceholder = new NestedValueParsedPlaceholder(nestedValue);

        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> value + "|second");

        assertEquals(nestedValue + "|first", parsedPlaceholder.getReplacement());
        assertEquals(nestedValue + "|first|second", PlaceholderParser.replacePlaceholders(nestedValue));
    }

    private long addProcessor(PlaceholderParser.ParsingProcessorTiming timing, ConsumingSupplier<String, String> processor) {
        long id = PlaceholderParser.addParsingProcessor(timing, processor);
        this.processorIds.add(id);
        return id;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for coordinated parser work");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for coordinated parser work", ex);
        }
    }

    private static final class NestedValueParsedPlaceholder extends PlaceholderParser.ParsedPlaceholder {

        private static final String SERIALIZED = "{\"placeholder\":\"nested_snapshot_test\"}";
        private static final Placeholder PLACEHOLDER = new PassthroughPlaceholder();
        private final String nestedValue;

        private NestedValueParsedPlaceholder(String nestedValue) {
            super(SERIALIZED, 0, SERIALIZED.length(), new HashMap<>(), false);
            this.nestedValue = nestedValue;
        }

        @Override
        public String getIdentifier() {
            return PLACEHOLDER.getIdentifier();
        }

        @Override
        public Placeholder getPlaceholder() {
            return PLACEHOLDER;
        }

        @Override
        public HashMap<String, String> getValues() {
            HashMap<String, String> values = new HashMap<>();
            values.put("text", this.nestedValue);
            return values;
        }

    }

    private static final class PassthroughPlaceholder extends Placeholder {

        private PassthroughPlaceholder() {
            super("nested_snapshot_test");
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            return dps.values.get("text");
        }

        @Override
        public List<String> getValueNames() {
            return List.of("text");
        }

        @Override
        public String getDisplayName() {
            return "Nested Snapshot Test";
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
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return new DeserializedPlaceholderString(this.getIdentifier(), new HashMap<>(), NestedValueParsedPlaceholder.SERIALIZED);
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
