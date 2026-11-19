package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.mainThreadValue;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Placeholders that can only get replaced on the main thread keep their raw text when a text gets parsed on another thread,
 * like by an Async-mode Ticker. Such results must never get cached, or the main thread would get them too.
 * The test thread stands in for the main thread.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserAsyncCacheTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;
    private ExecutorService worker;

    @BeforeEach
    void setUp() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        TestPlaceholders.setMainThread(Thread.currentThread());
        this.worker = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        this.worker.shutdownNow();
        assertTrue(this.worker.awaitTermination(5L, TimeUnit.SECONDS));
        TestPlaceholders.setMainThread(null);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void skippedPlaceholderDoesNotGetItsRawTextCached() throws Exception {
        TestPlaceholders.setValue("async_cache_top", "main value");
        String placeholder = mainThreadValue("async_cache_top");
        String text = "Value: " + placeholder;
        assertEquals("Value: " + placeholder, this.parseOnWorker(() -> PlaceholderParser.replacePlaceholders(text)));
        assertEquals("Value: main value", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void skippedPlaceholderInValueDoesNotGetItsRawTextCached() throws Exception {
        TestPlaceholders.setValue("async_cache_value", "main value");
        String placeholder = mainThreadValue("async_cache_value");
        String text = echo(placeholder) + "!";
        assertEquals(placeholder + "!", this.parseOnWorker(() -> PlaceholderParser.replacePlaceholders(text)));
        // The value has its own cache entry, which another text with the same value would get
        assertEquals("main value|x", PlaceholderParser.replacePlaceholders(join(placeholder, "x")));
        assertEquals("main value!", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void skippedPlaceholderInOutputDoesNotGetItsRawTextCached() throws Exception {
        TestPlaceholders.setValue("async_cache_output_target", "main value");
        TestPlaceholders.setValue("async_cache_output", mainThreadValue("async_cache_output_target"));
        String text = "Value: " + value("async_cache_output");
        assertEquals("Value: " + mainThreadValue("async_cache_output_target"), this.parseOnWorker(() -> PlaceholderParser.replacePlaceholders(text)));
        assertEquals("Value: main value", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void identicalSkippedPlaceholdersAllKeepTheirRawText() throws Exception {
        TestPlaceholders.setValue("async_cache_identical", "main value");
        String nested = echo(mainThreadValue("async_cache_identical"));
        String text = nested + " " + nested;
        String raw = mainThreadValue("async_cache_identical");
        assertEquals(raw + " " + raw, this.parseOnWorker(() -> PlaceholderParser.replacePlaceholders(text)));
        assertEquals("main value main value", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void resultsWithoutSkippedPlaceholdersStillGetCachedOnOtherThreads() throws Exception {
        TestPlaceholders.setValue("async_cache_normal", "first");
        String text = "Value: " + value("async_cache_normal");
        assertEquals("Value: first", this.parseOnWorker(() -> PlaceholderParser.replacePlaceholders(text)));
        TestPlaceholders.setValue("async_cache_normal", "second");
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        assertEquals(1, TestPlaceholders.evaluations("async_cache_normal"));
    }

    @Test
    void mainThreadGetsMainThreadOnlyPlaceholdersReplacedAndCached() {
        TestPlaceholders.setValue("async_cache_main", "first");
        String text = "Value: " + mainThreadValue("async_cache_main");
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
        TestPlaceholders.setValue("async_cache_main", "second");
        assertEquals("Value: first", PlaceholderParser.replacePlaceholders(text));
    }

    @NotNull
    private String parseOnWorker(@NotNull Supplier<String> parse) throws Exception {
        return this.worker.submit(parse::get).get(5L, TimeUnit.SECONDS);
    }

}
