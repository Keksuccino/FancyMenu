package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code $$name} references get resolved by {@link VariableStore#resolveReferences(String)} through a name index that only gets rebuilt
 * when the variable set changes. Every store here writes only to a temporary directory, and its debounced writes never run.
 */
class VariableStoreReferenceResolutionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void longestNameWinsWithOverlappingNames() {
        VariableStore store = this.createStore();
        store.setVariable("c", "C");
        store.setVariable("coins", "25");
        store.setVariable("coins_record", "80");
        store.setVariable("coins_record_today", "99");

        assertEquals("99|80|25|C|25X|Coins|C_record_x", store.resolveReferences("$$coins_record_today|$$coins_record|$$coins|$$c|$$coinsX|Coins|$$c_record_x"));
    }

    @Test
    void namesSharingFirstCharactersResolveTheSameInEveryInsertionOrder() {
        List<String> names = new ArrayList<>(List.of("a", "ab", "abc", "abd", "b", "ba", "abcd", "aX"));
        String text = "$$abcde $$abd $$abx $$a $$aX $$aXY $$bab $$b";
        String expected = "[abcd]e [abd] [ab]x [a] [aX] [aX]Y [ba]b [b]";
        Random random = new Random(1234L);
        for (int round = 0; round < 25; round++) {
            Collections.shuffle(names, random);
            VariableStore store = this.createStore();
            for (String name : names) store.setVariable(name, "[" + name + "]");
            assertEquals(expected, store.resolveReferences(text), "insertion order " + names);
        }
    }

    @Test
    void namesWithUnusualCharactersResolveLikeAnyOtherName() {
        VariableStore store = this.createStore();
        store.setVariable("my var", "spaces");
        store.setVariable("a.b-c", "symbols");
        store.setVariable("$", "dollar");
        store.setVariable("{\"x\"}", "json");
        store.setVariable("Grüße", "umlaut");
        store.setVariable("Grü", "short umlaut");
        store.setVariable("日本語", "cjk");
        store.setVariable("😀", "grinning");
        store.setVariable("😀😁", "two emoji");
        store.setVariable("😂x", "other emoji");
        store.setVariable("\uD83D", "lone high surrogate");

        assertAll(() -> assertEquals("spaces|symbols|json", store.resolveReferences("$$my var|$$a.b-c|$${\"x\"}")), () -> assertEquals("dollar", store.resolveReferences("$$$")), () -> assertEquals("dollardollar", store.resolveReferences("$$$$$$")), () -> assertEquals("umlaut|short umlautx|cjk!", store.resolveReferences("$$Grüße|$$Grüx|$$日本語!")), () -> assertEquals("two emoji|grinning😃|other emoji", store.resolveReferences("$$😀😁|$$😀😃|$$😂x")), () -> assertEquals("lone high surrogate\uDE03", store.resolveReferences("$$😃")));
    }

    @Test
    void emptyNamesNeverMatch() {
        VariableStore store = this.createStore();
        store.setVariable("", "empty");
        String text = "$$ and $$coins and $$";

        assertSame(text, store.resolveReferences(text));
        store.setVariable("coins", "25");
        assertEquals("$$ and 25 and $$", store.resolveReferences(text));
    }

    @Test
    void textsWithoutKnownReferencesStayTheSameInstance() {
        VariableStore store = this.createStore();
        String unknownReference = "$$unknown and $$ alone";
        String noReference = "coins";

        assertSame(unknownReference, store.resolveReferences(unknownReference));
        store.setVariable("coins", "25");
        assertAll(() -> assertSame(unknownReference, store.resolveReferences(unknownReference)), () -> assertSame(noReference, store.resolveReferences(noReference)));
    }

    @Test
    void replacementsAreNotSearchedForReferencesAgain() {
        VariableStore store = this.createStore();
        store.setVariable("pointer", "$$coins");
        store.setVariable("coins", "25");

        assertEquals("$$coins|25", store.resolveReferences("$$pointer|$$coins"));
    }

    @Test
    void valueChangesAreVisibleImmediatelyWithoutChangingTheVariableSet() {
        VariableStore store = this.createStore();
        store.setVariable("coins", "1");
        assertEquals("Coins: 1", store.resolveReferences("Coins: $$coins"));
        long revision = store.getVariableSetRevision();

        store.setVariable("coins", "2");
        assertEquals("Coins: 2", store.resolveReferences("Coins: $$coins"));
        Variable handle = store.getVariable("coins");
        assertNotNull(handle);
        handle.setValue("line one\nline two");
        assertEquals("Coins: line one\nline two", store.resolveReferences("Coins: $$coins"));
        handle.setValue(null);

        assertAll(() -> assertEquals("Coins: ", store.resolveReferences("Coins: $$coins")), () -> assertEquals(revision, store.getVariableSetRevision()));
    }

    @Test
    void variableSetRevisionIncreasesOnEveryChangeOfTheVariableSet() {
        VariableStore store = this.createStore();
        assertIncreases(store, s -> s.setVariable("coins", "25"));
        assertIncreases(store, s -> assertTrue(s.setVariableIfAbsent("coins_record", "80")));
        assertIncreases(store, s -> assertEquals("coins_Copy", s.createVariableWithUniqueCopyName("coins", "25", false)));
        assertIncreases(store, s -> s.removeVariable("coins_Copy"));
        assertIncreases(store, s -> s.replaceVariables(List.of(snapshot("coins", "25"), snapshot("coins_record", "80"))));
        assertIncreases(store, s -> s.replaceVariables(List.of()));
        store.setVariable("coins", "25");
        assertIncreases(store, VariableStore::clearVariables);
    }

    @Test
    void variableSetRevisionStaysWhenNoVariableGetsAddedOrRemoved() {
        VariableStore store = this.createStore();
        store.setVariable("coins", "25");
        Variable handle = store.getVariable("coins");
        assertNotNull(handle);

        assertStays(store, s -> s.setVariable("coins", "26"));
        assertStays(store, s -> s.setVariable("coins", "26"));
        assertStays(store, s -> assertFalse(s.setVariableIfAbsent("coins", "ignored")));
        assertStays(store, s -> s.removeVariable("missing"));
        assertStays(store, s -> handle.setValue("27"));
        assertStays(store, s -> handle.setResetOnLaunch(true));
        assertStays(store, s -> handle.toggleResetOnLaunch());
        assertStays(store, s -> s.resolveReferences("$$coins"));
        assertStays(store, VariableStore::flush);
        store.clearVariables();
        assertStays(store, VariableStore::clearVariables);

        store.setVariable("kept", "value");
        store.shutdown();
        assertStays(store, s -> {
            s.setVariable("late", "ignored");
            s.setVariableIfAbsent("late", "ignored");
            s.createVariableWithUniqueCopyName("kept", "ignored", false);
            s.removeVariable("kept");
            s.clearVariables();
            s.replaceVariables(List.of(snapshot("replacement", "ignored")));
            s.readFromFile();
        });
        assertEquals("value|$$late", store.resolveReferences("$$kept|$$late"));
    }

    @Test
    void initializationAndReloadsChangeTheVariableSet() throws Exception {
        Path target = this.temporaryDirectory.resolve("user_variables.db");
        Files.writeString(target, "type = user_variables\n\nvariable {\n  name = coins\n  value = 25\n  reset_on_launch = false\n}\n\nvariable {\n  name = coins_today\n  value = 3\n  reset_on_launch = true\n}\n", StandardCharsets.UTF_8);
        VariableStore store = this.createStore(target);

        assertIncreases(store, VariableStore::init);
        assertEquals("25||", store.resolveReferences("$$coins|$$coins_today|"));

        store.setVariable("memory_only", "gone after reload");
        assertTrue(store.flush());
        Files.writeString(target, "type = user_variables\n\nvariable {\n  name = coins_record\n  value = 80\n  reset_on_launch = false\n}\n", StandardCharsets.UTF_8);
        assertIncreases(store, VariableStore::readFromFile);
        assertEquals("$$coins|80|$$memory_only", store.resolveReferences("$$coins|$$coins_record|$$memory_only"));

        Files.writeString(target, "type = cached_variables\n\nvariables {\n  legacy_coins = 7\n}\n", StandardCharsets.UTF_8);
        assertIncreases(store, VariableStore::readFromLegacyFile);
        assertEquals("$$coins_record|7", store.resolveReferences("$$coins_record|$$legacy_coins"));
    }

    @Test
    void indexFollowsAddedRemovedRenamedAndReplacedVariables() {
        VariableStore store = this.createStore();
        store.setVariable("coins", "25");
        String text = "$$coins_record|$$coins|$$money";
        assertEquals("25_record|25|$$money", store.resolveReferences(text));

        store.setVariable("coins_record", "80");
        assertEquals("80|25|$$money", store.resolveReferences(text));

        store.removeVariable("coins_record");
        assertEquals("25_record|25|$$money", store.resolveReferences(text));

        // Renaming is removing the old name and creating the new one, like the variable manager does it.
        store.removeVariable("coins");
        store.setVariable("money", "25");
        assertEquals("$$coins_record|$$coins|25", store.resolveReferences(text));

        store.replaceVariables(List.of(snapshot("coins", "1"), snapshot("money", "2")));
        assertEquals("1_record|1|2", store.resolveReferences(text));

        store.clearVariables();
        assertSame(text, store.resolveReferences(text));

        assertEquals("coins_Copy", store.createVariableWithUniqueCopyName("coins", "copy", false));
        assertTrue(store.setVariableIfAbsent("coins", "original"));
        assertEquals("original_record|original|$$money|copy", store.resolveReferences(text + "|$$coins_Copy"));
    }

    @Test
    void listenersThatResolveReferencesWhileTheVariableSetChangesNeverSeeAnOutdatedIndex() {
        AtomicReference<VariableStore> storeReference = new AtomicReference<>();
        List<String> resolvedInListener = new ArrayList<>();
        VariableStore store = this.createStore(this.temporaryDirectory.resolve("user_variables.db"), (name, oldValue, newValue) -> {
            if (name.startsWith("coins")) resolvedInListener.add(storeReference.get().resolveReferences("$$coins_record"));
        });
        storeReference.set(store);
        assertEquals("$$coins_record", store.resolveReferences("$$coins_record"));

        store.setVariable("coins_record", "created");
        store.replaceVariables(List.of(snapshot("coins", "25"), snapshot("coins_record", "80")));

        assertAll(() -> assertEquals(List.of("created", "25_record", "80"), resolvedInListener), () -> assertEquals("80", store.resolveReferences("$$coins_record")));
    }

    @Test
    void failedReplacementRestoresTheIndexOfThePreviousVariables() {
        AtomicReference<VariableStore> storeReference = new AtomicReference<>();
        AtomicBoolean failing = new AtomicBoolean();
        VariableStore store = this.createStore(this.temporaryDirectory.resolve("user_variables.db"), (name, oldValue, newValue) -> {
            if (failing.get() && "boom".equals(name)) {
                // Builds the index of the half-replaced variable set right before the replacement fails.
                assertEquals("1|2", storeReference.get().resolveReferences("$$coins|$$boom"));
                throw new IllegalStateException("simulated listener failure");
            }
        });
        storeReference.set(store);
        store.setVariable("coins", "25");
        assertEquals("25|$$boom", store.resolveReferences("$$coins|$$boom"));
        failing.set(true);

        assertThrows(IllegalStateException.class, () -> store.replaceVariables(List.of(snapshot("coins", "1"), snapshot("boom", "2"))));

        assertEquals("25|$$boom", store.resolveReferences("$$coins|$$boom"));
    }

    @Test
    void resolutionMatchesSnapshotBasedResolutionForGeneratedTextsAndVariableSets() {
        char[] alphabet = {'a', 'b', 'c', '_', '$', ' ', 'ü', '{', '"', '%', 'n', '\uD83D', '\uDE00', '\uDE01', '日'};
        Random random = new Random(987654321L);
        int comparisons = 0;
        for (int round = 0; round < 150; round++) {
            VariableStore store = this.createStore();
            List<String> names = new ArrayList<>();
            int variableCount = random.nextInt(25);
            for (int i = 0; i < variableCount; i++) {
                String name = randomName(random, alphabet, names);
                names.add(name);
                store.setVariable(name, randomValue(random, alphabet, names));
            }
            for (int step = 0; step < 12; step++) {
                for (int textIndex = 0; textIndex < 20; textIndex++) {
                    String text = randomText(random, alphabet, names);
                    String expected = resolveWithSnapshots(text, store.getVariableSnapshots());
                    String actual = store.resolveReferences(text);
                    assertEquals(expected, actual, () -> "text '" + text + "' with variables " + store.getVariableSnapshots());
                    if (expected == text) assertSame(text, actual);
                    comparisons++;
                }
                this.mutateRandomly(store, random, alphabet, names);
            }
        }
        assertEquals(150 * 12 * 20, comparisons);
    }

    @Test
    void concurrentReadersAlwaysSeeOneCompleteVariableSetWhileItChanges() throws Exception {
        VariableStore store = this.createStore();
        store.replaceVariables(generation(0));
        int readerCount = 3;
        int generations = 1500;
        CountDownLatch ready = new CountDownLatch(readerCount + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean writerDone = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + 1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    for (int generation = 1; generation <= generations; generation++) {
                        store.replaceVariables(generation(generation));
                        store.setVariable("counter", Integer.toString(generation));
                    }
                } finally {
                    writerDone.set(true);
                }
                return null;
            }));
            for (int reader = 0; reader < readerCount; reader++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    int checks = 0;
                    while (!writerDone.get() || (checks < 100)) {
                        String[] parts = store.resolveReferences("$$gen_a|$$gen_b|$$gen_a_long").split("\\|", -1);
                        int generation = Integer.parseInt(parts[0]);
                        assertEquals(parts[0], parts[1]);
                        assertEquals(((generation % 2) == 0) ? parts[0] : (parts[0] + "_long"), parts[2]);
                        checks++;
                    }
                    return null;
                }));
            }
            boolean allTasksReady = ready.await(5L, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(allTasksReady);
            for (Future<?> future : futures) future.get(30L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10L, TimeUnit.SECONDS));
        }
        assertEquals(generations + "|" + generations + "|" + generations + "|" + generations, store.resolveReferences("$$gen_a|$$gen_b|$$gen_a_long|$$counter"));
    }

    @Test
    void concurrentReadersSeeValueChangesInOrder() throws Exception {
        VariableStore store = this.createStore();
        store.setVariable("counter", "0");
        store.setVariable("counter_label", "Counter");
        int readerCount = 2;
        int updates = 20000;
        CountDownLatch ready = new CountDownLatch(readerCount + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean writerDone = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + 1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    for (int value = 1; value <= updates; value++) store.setVariable("counter", Integer.toString(value));
                } finally {
                    writerDone.set(true);
                }
                return null;
            }));
            for (int reader = 0; reader < readerCount; reader++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    int last = 0;
                    int checks = 0;
                    while (!writerDone.get() || (checks < 100)) {
                        String resolved = store.resolveReferences("$$counter_label: $$counter");
                        assertTrue(resolved.startsWith("Counter: "), resolved);
                        int value = Integer.parseInt(resolved.substring("Counter: ".length()));
                        assertTrue(value >= last, () -> value + " after " + resolved);
                        last = value;
                        checks++;
                    }
                    return null;
                }));
            }
            boolean allTasksReady = ready.await(5L, TimeUnit.SECONDS);
            start.countDown();
            assertTrue(allTasksReady);
            for (Future<?> future : futures) future.get(30L, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10L, TimeUnit.SECONDS));
        }
        assertEquals("Counter: " + updates, store.resolveReferences("$$counter_label: $$counter"));
    }

    private void mutateRandomly(@NotNull VariableStore store, @NotNull Random random, @NotNull char[] alphabet, @NotNull List<String> names) {
        int operation = random.nextInt(10);
        if ((operation <= 2) || names.isEmpty()) {
            String name = randomName(random, alphabet, names);
            names.add(name);
            store.setVariable(name, randomValue(random, alphabet, names));
        } else if (operation <= 4) {
            store.removeVariable(names.remove(random.nextInt(names.size())));
        } else if (operation <= 7) {
            store.setVariable(names.get(random.nextInt(names.size())), randomValue(random, alphabet, names));
        } else if (operation == 8) {
            List<UserVariableSnapshot> replacement = new ArrayList<>();
            List<String> kept = new ArrayList<>();
            for (String name : names) {
                if (random.nextBoolean()) {
                    kept.add(name);
                    replacement.add(snapshot(name, randomValue(random, alphabet, names)));
                }
            }
            names.clear();
            names.addAll(kept);
            store.replaceVariables(replacement);
        } else {
            store.clearVariables();
            names.clear();
        }
    }

    @NotNull
    private static String randomName(@NotNull Random random, @NotNull char[] alphabet, @NotNull List<String> existing) {
        while (true) {
            String name;
            if (!existing.isEmpty() && random.nextInt(3) == 0) {
                // Extends or shortens an existing name, so names start with other names.
                String base = existing.get(random.nextInt(existing.size()));
                name = random.nextBoolean() ? (base + alphabet[random.nextInt(alphabet.length)]) : base.substring(0, random.nextInt(base.length() + 1));
            } else {
                StringBuilder builder = new StringBuilder();
                int length = random.nextInt(5);
                for (int i = 0; i < length; i++) builder.append(alphabet[random.nextInt(alphabet.length)]);
                name = builder.toString();
            }
            if (!existing.contains(name)) return name;
        }
    }

    @NotNull
    private static String randomValue(@NotNull Random random, @NotNull char[] alphabet, @NotNull List<String> names) {
        int kind = random.nextInt(6);
        if (kind == 0) return "";
        if ((kind == 1) && !names.isEmpty()) return "$$" + names.get(random.nextInt(names.size()));
        if (kind == 2) return "line\nbreak";
        StringBuilder builder = new StringBuilder();
        int length = 1 + random.nextInt(4);
        for (int i = 0; i < length; i++) builder.append(alphabet[random.nextInt(alphabet.length)]);
        return builder.toString();
    }

    @NotNull
    private static String randomText(@NotNull Random random, @NotNull char[] alphabet, @NotNull List<String> names) {
        StringBuilder builder = new StringBuilder();
        int tokens = random.nextInt(14);
        for (int i = 0; i < tokens; i++) {
            int kind = random.nextInt(5);
            if ((kind <= 1) && !names.isEmpty()) {
                builder.append("$$").append(names.get(random.nextInt(names.size())));
            } else if (kind == 2) {
                builder.append(random.nextBoolean() ? "$$" : "$");
            } else {
                builder.append(alphabet[random.nextInt(alphabet.length)]);
            }
        }
        return builder.toString();
    }

    /** The resolution of {@code $$name} references before the name index: one snapshot of all variables, every name checked per reference. */
    @NotNull
    private static String resolveWithSnapshots(@NotNull String text, @NotNull List<UserVariableSnapshot> variables) {
        if (variables.isEmpty()) return text;
        return ShortReferenceResolver.resolve(text, (in, nameStart) -> {
            UserVariableSnapshot variable = ShortReferenceResolver.findLongestName(in, nameStart, variables, UserVariableSnapshot::name);
            return (variable != null) ? new ShortReferenceResolver.Reference(variable.name().length(), variable.value()) : null;
        });
    }

    private static void assertIncreases(@NotNull VariableStore store, @NotNull Consumer<VariableStore> operation) {
        long before = store.getVariableSetRevision();
        operation.accept(store);
        long after = store.getVariableSetRevision();
        assertTrue(after > before, () -> "revision " + before + " -> " + after);
    }

    private static void assertStays(@NotNull VariableStore store, @NotNull Consumer<VariableStore> operation) {
        long before = store.getVariableSetRevision();
        operation.accept(store);
        assertEquals(before, store.getVariableSetRevision());
    }

    @NotNull
    private static List<UserVariableSnapshot> generation(int generation) {
        String value = Integer.toString(generation);
        List<UserVariableSnapshot> snapshots = new ArrayList<>();
        snapshots.add(snapshot("gen_a", value));
        snapshots.add(snapshot("gen_b", value));
        if ((generation % 2) == 0) snapshots.add(snapshot("gen_a_long", value));
        snapshots.add(snapshot("counter", value));
        return snapshots;
    }

    @NotNull
    private static UserVariableSnapshot snapshot(@NotNull String name, @NotNull String value) {
        return new UserVariableSnapshot(name, value, false);
    }

    @NotNull
    private VariableStore createStore() {
        return this.createStore(this.temporaryDirectory.resolve("user_variables.db"));
    }

    @NotNull
    private VariableStore createStore(@NotNull Path target) {
        return this.createStore(target, (name, oldValue, newValue) -> {});
    }

    @NotNull
    private VariableStore createStore(@NotNull Path target, @NotNull VariableStore.VariableUpdateListener listener) {
        return new VariableStore(new AtomicVariableDatabase(target), listener, new HeldScheduler());
    }

    /** Keeps debounced writes from ever running, so only explicit flushes and reloads touch the temporary database. */
    private static final class HeldScheduler implements VariablePersistenceCoordinator.Scheduler {

        @Override
        public @NotNull VariablePersistenceCoordinator.ScheduledTask schedule(@NotNull Runnable task, long delayMillis) {
            return () -> {};
        }

        @Override
        public void shutdown() {
        }

    }

}
