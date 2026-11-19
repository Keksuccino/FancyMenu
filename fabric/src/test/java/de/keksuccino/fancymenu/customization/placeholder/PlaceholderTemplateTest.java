package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Texts get compiled into {@link PlaceholderTemplate}s once, which stay valid until processors or registered placeholders change,
 * and can be compiled and replaced by many threads at the same time.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderTemplateTest {

    private final List<Long> processorIds = new ArrayList<>();
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreGlobalState() {
        for (long id : this.processorIds) PlaceholderParser.removeParsingProcessor(id);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void templateHoldsOnlyPlaceholdersThatCanGetReplaced() {
        String echo = echo(echo("nested"));
        String join = join("plain", value("template_test_nodes"));
        String text = "a {\"placeholder\":\"template_test_unknown\"} " + echo + " {\"placeholder\":\"\"} " + join + " b";
        PlaceholderTemplate template = PlaceholderTemplate.compile(text, context());
        assertEquals(2, template.nodes.length);
        PlaceholderTemplate.Node first = template.nodes[0];
        assertEquals(echo, first.raw);
        assertEquals(echo, text.substring(first.start, first.end));
        assertEquals(TestPlaceholders.echoIdentifier(), first.identifier);
        assertSame(PlaceholderRegistry.getPlaceholder(TestPlaceholders.echoIdentifier()), first.placeholder);
        assertEquals(1, first.values.length);
        assertNotNull(first.values[0].template);
        assertEquals(1, first.values[0].template.nodes.length);
        PlaceholderTemplate.Node second = template.nodes[1];
        assertEquals(join, second.raw);
        Map<String, PlaceholderTemplate.ValueSlot> values = new HashMap<>();
        for (PlaceholderTemplate.ValueSlot slot : second.values) values.put(slot.name, slot);
        assertNull(values.get("first").template);
        assertEquals("plain", values.get("first").raw);
        assertNotNull(values.get("second").template);
    }

    @Test
    void cachedTemplateGetsReusedForTheSameText() {
        String text = "reuse " + echo("x");
        PlaceholderEvaluator.ParsingContext context = context();
        assertSame(PlaceholderTemplate.get(text, context), PlaceholderTemplate.get(text, context));
        assertSame(PlaceholderTemplate.get(text, context), PlaceholderTemplate.get(new String(text.toCharArray()), context));
    }

    @Test
    void processorChangesMakeCachedTemplatesInvalid() {
        String text = "processors " + echo("x");
        PlaceholderTemplate before = PlaceholderTemplate.get(text, context());
        long id = this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> in);
        PlaceholderEvaluator.ParsingContext added = context();
        PlaceholderTemplate afterAdding = PlaceholderTemplate.get(text, added);
        assertNotSame(before, afterAdding);
        assertEquals(added.processors().revision(), afterAdding.processorRevision);
        PlaceholderParser.removeParsingProcessor(id);
        assertNotSame(afterAdding, PlaceholderTemplate.get(text, context()));
    }

    @Test
    void laterRegisteredPlaceholderGetsReplacedInCachedTexts() {
        String identifier = "template_test_late_registration";
        String text = "late {\"placeholder\":\"" + identifier + "\"} " + echo("x");
        assertEquals(text.replace(echo("x"), "x"), PlaceholderParser.replacePlaceholders(text));
        PlaceholderTemplate before = PlaceholderTemplate.get(text, context());
        assertEquals(1, before.nodes.length);
        registerConstant(identifier, "now known");
        PlaceholderTemplate after = PlaceholderTemplate.get(text, context());
        assertNotSame(before, after);
        assertEquals(2, after.nodes.length);
        assertEquals("late now known x", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void valuesGetCompiledAfterTheBeforeProcessorsRan() {
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, in -> in.equals("template_test_before") ? echo("from processor") : in);
        assertEquals("from processor|x", PlaceholderParser.replacePlaceholders(join("template_test_before", "x")));
    }

    @Test
    void valueThatAnEarlierCompileProcessedGetsKeptOnlyOnce() {
        // The text editor's compile processor returns the instance it cached when the value got compiled for another text before
        String value = "template_test_kept_once " + echo("x");
        PlaceholderTemplate first = PlaceholderTemplate.compile("first " + echo(value), context());
        PlaceholderTemplate second = PlaceholderTemplate.compile("second " + echo(value), context());
        PlaceholderTemplate.ValueSlot firstSlot = first.nodes[0].values[0];
        PlaceholderTemplate.ValueSlot secondSlot = second.nodes[0].values[0];

        assertSame(firstSlot.processed, secondSlot.processed);
        assertSame(secondSlot.processed, secondSlot.raw);
        assertEquals("second template_test_kept_once x", PlaceholderParser.replacePlaceholders("second " + echo(value)));
    }

    @Test
    void beforeProcessorsRunOnValuesAtEveryParse() {
        AtomicReference<String> processed = new AtomicReference<>("one");
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, in -> in.equals("template_test_dynamic") ? processed.get() : in);
        String text = join("template_test_dynamic", echo("x"));
        assertEquals("one|x", PlaceholderParser.replacePlaceholders(text));
        processed.set(echo("two"));
        assertEquals("two|x", PlaceholderParser.replacePlaceholders(text));
        processed.set("three");
        assertEquals("three|x", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void textsGetCompiledAndReplacedByManyThreadsAtOnce() throws Exception {
        int threads = 8;
        int sharedTexts = 64;
        String[] texts = new String[sharedTexts];
        String[] expected = new String[sharedTexts];
        for (int i = 0; i < sharedTexts; i++) {
            TestPlaceholders.setValue("template_concurrency_" + i, "v" + i);
            texts[i] = "T" + i + ": " + join(echo("a " + i), value("template_concurrency_" + i)) + " " + echo(echo("b" + i)) + " " + value("template_concurrency_" + i);
            expected[i] = "T" + i + ": a " + i + "|v" + i + " b" + i + " v" + i;
        }
        AtomicBoolean registering = new AtomicBoolean(true);
        ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                int thread = t;
                futures.add(executor.submit(() -> {
                    await(start);
                    for (int iteration = 0; iteration < 3000; iteration++) {
                        int index = (iteration * 31 + thread * 7) % sharedTexts;
                        assertEquals(expected[index], PlaceholderParser.replacePlaceholders(texts[index]));
                        // A text no other thread parses, so it gets compiled while other threads use the cache
                        String unique = "U" + thread + "/" + iteration + " " + echo("u" + iteration);
                        assertEquals("U" + thread + "/" + iteration + " u" + iteration, PlaceholderParser.replacePlaceholders(unique));
                    }
                    return null;
                }));
            }
            // New registrations make all cached templates invalid while the other threads use them
            futures.add(executor.submit(() -> {
                await(start);
                for (int i = 0; (i < 50) && registering.get(); i++) {
                    registerConstant("template_concurrency_late_" + i, "late" + i);
                    Thread.yield();
                }
                return null;
            }));
            start.countDown();
            for (Future<?> future : futures) future.get(60L, TimeUnit.SECONDS);
        } finally {
            registering.set(false);
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
        assertEquals("late0 late49", PlaceholderParser.replacePlaceholders("{\"placeholder\":\"template_concurrency_late_0\"} {\"placeholder\":\"template_concurrency_late_49\"}"));
    }

    private long addProcessor(@NotNull PlaceholderParser.ParsingProcessorTiming timing, @NotNull ConsumingSupplier<String, String> processor) {
        long id = PlaceholderParser.addParsingProcessor(timing, processor);
        this.processorIds.add(id);
        return id;
    }

    @NotNull
    private static PlaceholderEvaluator.ParsingContext context() {
        return PlaceholderEvaluator.createParsingContext(false, true);
    }

    private static void await(@NotNull CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for the start");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the start", ex);
        }
    }

    private static void registerConstant(@NotNull String identifier, @NotNull String replacement) {
        if (PlaceholderRegistry.getPlaceholder(identifier) != null) return;
        PlaceholderRegistry.register(new Placeholder(identifier) {

            @Override
            public String getReplacementFor(DeserializedPlaceholderString dps) {
                return replacement;
            }

            @Override
            public List<String> getValueNames() {
                return null;
            }

            @Override
            public @NotNull String getDisplayName() {
                return identifier;
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
                return new DeserializedPlaceholderString(identifier, new HashMap<>(), "{\"placeholder\":\"" + identifier + "\"}");
            }

            @Override
            public boolean checkAsync() {
                return true;
            }

        });
    }

}
