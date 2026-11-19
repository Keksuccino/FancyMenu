package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.StringifyPlaceholder;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.nestedParse;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inside a {@link FreshValuesScope}, the parser must not answer from its result caches, but still store its fresh results for rendering.
 * Caching is on with a caching duration of a minute, so every cached result stays usable during the whole test.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserFreshValuesScopeTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;
    private ExecutorService worker;

    @BeforeEach
    void setUp() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        this.worker = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        this.worker.shutdownNow();
        assertTrue(this.worker.awaitTermination(5L, TimeUnit.SECONDS));
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
        assertFalse(FreshValuesScope.isActive());
    }

    @Test
    void textGetsParsedAgainInsideTheScopeAndItsFreshResultGetsCached() {
        String key = "fresh_scope_text";
        String text = "Value: " + value(key);
        TestPlaceholders.setValue(key, "first");
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        TestPlaceholders.setValue(key, "second");
        // Rendering keeps getting the cached result
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        assertEquals(1, TestPlaceholders.evaluations(key));

        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("Value: second", PlaceholderParser.replacePlaceholders(text));
            TestPlaceholders.setValue(key, "third");
            assertEquals("Value: third", PlaceholderParser.replacePlaceholdersWithoutLengthLimit(text, false));
        }
        assertEquals(3, TestPlaceholders.evaluations(key));

        // Rendering gets the last fresh result from the cache
        TestPlaceholders.setValue(key, "fourth");
        assertEquals("Value: third", PlaceholderParser.replacePlaceholders(text));
        assertEquals(3, TestPlaceholders.evaluations(key));
    }

    @Test
    void valuesGetParsedAgainInsideTheScopeAndTheirFreshResultsGetCached() {
        String key = "fresh_scope_value";
        String placeholder = value(key);
        TestPlaceholders.setValue(key, "first");
        assertEquals("first!", PlaceholderParser.replacePlaceholders(echo(placeholder) + "!"));
        TestPlaceholders.setValue(key, "second");
        // Every text is new, so only the value can come from the cache, which another text with the same value gets
        assertEquals("first|a", PlaceholderParser.replacePlaceholders(join(placeholder, "a")));

        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("second|b", PlaceholderParser.replacePlaceholders(join(placeholder, "b")));
        }

        TestPlaceholders.setValue(key, "third");
        assertEquals("second|c", PlaceholderParser.replacePlaceholders(join(placeholder, "c")));
        assertEquals(2, TestPlaceholders.evaluations(key));
    }

    @Test
    void identicalPlaceholdersStillShareOneReplacementInsideTheScope() {
        String key = "fresh_scope_identical";
        String placeholder = value(key);
        TestPlaceholders.setValue(key, "x");

        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("x x|x", PlaceholderParser.replacePlaceholders(placeholder + " " + join(placeholder, echo(placeholder))));
        }

        assertEquals(1, TestPlaceholders.evaluations(key));
    }

    @Test
    void parseThatStringifyStartsItselfReadsTheScopeOfItsThread() {
        String key = "fresh_scope_stringify";
        String placeholder = value(key);
        TestPlaceholders.setValue(key, "first");
        assertEquals("first", PlaceholderParser.replacePlaceholders(placeholder));
        TestPlaceholders.setValue(key, "second");
        StringifyPlaceholder stringify = new StringifyPlaceholder();
        HashMap<String, String> values = new HashMap<>();
        values.put("text", placeholder);
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(stringify.getIdentifier(), values, "");

        assertEquals("first", stringify.getReplacementFor(dps));
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("second", stringify.getReplacementFor(dps));
        }

        TestPlaceholders.setValue(key, "third");
        assertEquals("second", PlaceholderParser.replacePlaceholders(placeholder));
    }

    @Test
    void parseThatAPlaceholderStartsItselfDoesNotEndTheScopeOfTheParseAroundIt() {
        String nestedKey = "fresh_scope_nested_parse";
        String valueKey = "fresh_scope_after_nested_parse";
        TestPlaceholders.setValue(nestedKey, "first");
        TestPlaceholders.setValue(valueKey, "first");
        assertEquals("first", PlaceholderParser.replacePlaceholders(value(nestedKey)));
        assertEquals("first", PlaceholderParser.replacePlaceholders(echo(value(valueKey))));
        TestPlaceholders.setValue(nestedKey, "second");
        TestPlaceholders.setValue(valueKey, "second");

        // Placeholders get replaced from the last to the first one, so the outer parse reads the value of echo after the nested parse ended
        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("second second", PlaceholderParser.replacePlaceholders(echo(value(valueKey)) + " " + nestedParse(nestedKey)));
        }

        TestPlaceholders.setValue(nestedKey, "third");
        assertEquals("second", PlaceholderParser.replacePlaceholders(value(nestedKey)));
    }

    @Test
    void foundPlaceholderReadsTheScopeOfTheMomentItGetsReplaced() {
        String key = "fresh_scope_found";
        String text = echo(value(key));
        TestPlaceholders.setValue(key, "first");
        assertEquals("first", PlaceholderParser.replacePlaceholders(text));
        TestPlaceholders.setValue(key, "second");
        PlaceholderParser.ParsedPlaceholder foundOutside = PlaceholderParser.findPlaceholders(text, new HashMap<>()).get(0);
        PlaceholderParser.ParsedPlaceholder foundInside;

        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("second", foundOutside.getReplacement());
            foundInside = PlaceholderParser.findPlaceholders(text, new HashMap<>()).get(0);
        }

        TestPlaceholders.setValue(key, "third");
        assertEquals("second", foundInside.getReplacement());
    }

    @Test
    void scopeOfAnotherThreadDoesNotBypassTheCache() throws Exception {
        String key = "fresh_scope_other_thread";
        String text = "Value: " + value(key);
        TestPlaceholders.setValue(key, "first");
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        TestPlaceholders.setValue(key, "second");

        try (FreshValuesScope ignored = FreshValuesScope.enter()) {
            assertEquals("Value: first", this.worker.submit(() -> PlaceholderParser.replacePlaceholders(text)).get(5L, TimeUnit.SECONDS));
        }

        CountDownLatch scopeOpen = new CountDownLatch(1);
        CountDownLatch parsedOutside = new CountDownLatch(1);
        Future<String> parsedInside = this.worker.submit(() -> {
            try (FreshValuesScope ignored = FreshValuesScope.enter()) {
                scopeOpen.countDown();
                await(parsedOutside);
                return PlaceholderParser.replacePlaceholders(text);
            }
        });
        try {
            await(scopeOpen);
            assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        } finally {
            parsedOutside.countDown();
        }
        assertEquals("Value: second", parsedInside.get(5L, TimeUnit.SECONDS));
    }

    private static void await(@NotNull CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5L, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for the other thread");
    }

}
