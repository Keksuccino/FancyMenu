package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import de.keksuccino.fancymenu.testing.ManualTaskQueue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomTextPlaceholderTest {

    @Test
    void concurrentFileMissesAdmitExactlyOneLoadAndPublishAnImmutableValue() throws Exception {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        List<String> mutableResult = new ArrayList<>(List.of("first", "second"));
        RandomTextPlaceholder placeholder = new RandomTextPlaceholder(tasks::add, source -> {
            loads.incrementAndGet();
            return mutableResult;
        }, () -> 100L);

        List<List<String>> results = ConcurrentTestCalls.invoke(32, () -> placeholder.getCachedOrLoadContent("config/value.txt"));

        assertTrue(results.stream().allMatch(value -> value == null));
        assertEquals(1, tasks.size());
        assertEquals(0, loads.get());
        assertTrue(placeholder.isSourceLoading("config/value.txt"));
        tasks.runNext();
        mutableResult.set(0, "mutated");
        mutableResult.add("third");
        List<String> cached = placeholder.getCachedOrLoadContent("config/value.txt");
        assertEquals(List.of("first", "second"), cached);
        assertThrows(UnsupportedOperationException.class, () -> cached.add("not allowed"));
        assertEquals(1, loads.get());
        assertFalse(placeholder.isSourceLoading("config/value.txt"));
    }

    @Test
    void classifiesPlainFileAndUrlSourcesAndPreservesTheirRefreshRules() {
        assertTrue(RandomTextPlaceholder.isPlainText("first\\nsecond"));
        assertTrue(RandomTextPlaceholder.isPlainText("single value"));
        assertFalse(RandomTextPlaceholder.isPlainText("config/value.txt"));
        assertFalse(RandomTextPlaceholder.isPlainText("https://example.invalid/value.txt"));
        assertFalse(RandomTextPlaceholder.isPlainText("http://example.invalid/value.txt"));
        assertTrue(RandomTextPlaceholder.isUrl("https://example.invalid/value.txt"));
        assertEquals(List.of("first", "second", "third"), RandomTextPlaceholder.parsePlainText("first\\nsecond\\nthird"));

        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        RandomTextPlaceholder placeholder = new RandomTextPlaceholder(tasks::add, source -> RandomTextPlaceholder.isPlainText(source) ? RandomTextPlaceholder.parsePlainText(source) : List.of(source), time::get);
        String plain = "first\\nsecond";
        String file = "config/value.txt";
        String url = "https://example.invalid/value.txt";

        assertNull(placeholder.getCachedOrLoadContent(plain));
        assertNull(placeholder.getCachedOrLoadContent(file));
        assertNull(placeholder.getCachedOrLoadContent(url));
        assertEquals(3, tasks.size());
        tasks.runNext();
        tasks.runNext();
        tasks.runNext();
        assertEquals(List.of("first", "second"), placeholder.getCachedOrLoadContent(plain));
        assertEquals(List.of(file), placeholder.getCachedOrLoadContent(file));
        assertEquals(List.of(url), placeholder.getCachedOrLoadContent(url));

        time.set(30000L);
        assertEquals(List.of("first", "second"), placeholder.getCachedOrLoadContent(plain));
        assertEquals(List.of(file), placeholder.getCachedOrLoadContent(file));
        assertEquals(List.of(url), placeholder.getCachedOrLoadContent(url));
        assertEquals(2, tasks.size());
    }

    @Test
    void failedFileReadCachesEmptyUntilCooldownThenRetries() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        AtomicInteger attempts = new AtomicInteger();
        RandomTextPlaceholder placeholder = new RandomTextPlaceholder(tasks::add, source -> {
            if (attempts.incrementAndGet() == 1) throw new IOException("expected test failure");
            return List.of("recovered");
        }, time::get);

        assertNull(placeholder.getCachedOrLoadContent("config/value.txt"));
        tasks.runNext();
        assertEquals(List.of(), placeholder.getCachedOrLoadContent("config/value.txt"));
        time.set(29999L);
        assertEquals(List.of(), placeholder.getCachedOrLoadContent("config/value.txt"));
        assertEquals(0, tasks.size());
        time.set(30000L);
        assertEquals(List.of(), placeholder.getCachedOrLoadContent("config/value.txt"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("recovered"), placeholder.getCachedOrLoadContent("config/value.txt"));
        assertEquals(2, attempts.get());
    }

    @Test
    void failedPlainTextResultKeepsItsNonExpiringLegacyCacheBoundary() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        AtomicInteger attempts = new AtomicInteger();
        RandomTextPlaceholder placeholder = new RandomTextPlaceholder(tasks::add, source -> {
            attempts.incrementAndGet();
            throw new IOException("expected test failure");
        }, time::get);

        assertNull(placeholder.getCachedOrLoadContent("plain value"));
        tasks.runNext();
        time.set(Long.MAX_VALUE);
        assertEquals(List.of(), placeholder.getCachedOrLoadContent("plain value"));
        assertEquals(0, tasks.size());
        assertEquals(1, attempts.get());
    }

    @Test
    void clearingCacheCancelsQueuedWorkWithoutReleasingTheNewClaim() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        RandomTextPlaceholder placeholder = new RandomTextPlaceholder(tasks::add, source -> List.of("value-" + loads.incrementAndGet()), () -> 0L);

        placeholder.getCachedOrLoadContent("config/value.txt");
        Runnable cancelledTask = tasks.removeNext();
        placeholder.clearContentCache();
        placeholder.getCachedOrLoadContent("config/value.txt");
        Runnable currentTask = tasks.removeNext();

        cancelledTask.run();
        assertEquals(0, loads.get());
        assertTrue(placeholder.isSourceLoading("config/value.txt"));
        currentTask.run();
        assertEquals(1, loads.get());
        assertEquals(List.of("value-1"), placeholder.getCachedOrLoadContent("config/value.txt"));
    }

}
