package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.testing.ConcurrentTestCalls;
import de.keksuccino.fancymenu.testing.ManualTaskQueue;
import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileTextPlaceholderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void concurrentFileMissesAdmitExactlyOneLoadAndPublishAnImmutableValue() throws Exception {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        List<String> mutableResult = new ArrayList<>(List.of("first", "second"));
        FileTextPlaceholder placeholder = new FileTextPlaceholder(tasks::add, source -> {
            loads.incrementAndGet();
            return mutableResult;
        }, () -> 100L);

        List<List<String>> results = ConcurrentTestCalls.invoke(32, () -> placeholder.getCachedOrLoadAsync("config/value.txt"));

        assertTrue(results.stream().allMatch(value -> value == null));
        assertEquals(1, tasks.size());
        assertEquals(0, loads.get());
        assertTrue(placeholder.isSourceLoading("config/value.txt"));
        tasks.runNext();
        mutableResult.set(0, "mutated");
        mutableResult.add("third");
        List<String> cached = placeholder.getCachedOrLoadAsync("config/value.txt");
        assertEquals(List.of("first", "second"), cached);
        assertThrows(UnsupportedOperationException.class, () -> cached.add("not allowed"));
        assertEquals(1, loads.get());
        assertFalse(placeholder.isSourceLoading("config/value.txt"));
    }

    @Test
    void fileAndUrlSourcesUseTheirLegacyRefreshIntervalsAndReturnStaleDataDuringRefresh() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        AtomicInteger fileLoads = new AtomicInteger();
        AtomicInteger urlLoads = new AtomicInteger();
        FileTextPlaceholder placeholder = new FileTextPlaceholder(tasks::add, source -> List.of(source.startsWith("http") ? "url-" + urlLoads.incrementAndGet() : "file-" + fileLoads.incrementAndGet()), time::get);

        assertNull(placeholder.getCachedOrLoadAsync("config/value.txt"));
        tasks.runNext();
        assertNull(placeholder.getCachedOrLoadAsync("https://example.invalid/value.txt"));
        tasks.runNext();
        assertEquals(List.of("file-1"), placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(List.of("url-1"), placeholder.getCachedOrLoadAsync("https://example.invalid/value.txt"));

        time.set(1000L);
        assertEquals(List.of("file-1"), placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(List.of("url-1"), placeholder.getCachedOrLoadAsync("https://example.invalid/value.txt"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("file-2"), placeholder.getCachedOrLoadAsync("config/value.txt"));

        time.set(10000L);
        assertEquals(List.of("url-1"), placeholder.getCachedOrLoadAsync("https://example.invalid/value.txt"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("url-2"), placeholder.getCachedOrLoadAsync("https://example.invalid/value.txt"));
    }

    @Test
    void failedReadCachesEmptyUntilCooldownThenRetries() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicLong time = new AtomicLong();
        AtomicInteger attempts = new AtomicInteger();
        FileTextPlaceholder placeholder = new FileTextPlaceholder(tasks::add, source -> {
            if (attempts.incrementAndGet() == 1) throw new IOException("expected test failure");
            return List.of("recovered");
        }, time::get);

        assertNull(placeholder.getCachedOrLoadAsync("config/value.txt"));
        tasks.runNext();
        assertEquals(List.of(), placeholder.getCachedOrLoadAsync("config/value.txt"));
        time.set(999L);
        assertEquals(List.of(), placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(0, tasks.size());
        time.set(1000L);
        assertEquals(List.of(), placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("recovered"), placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(2, attempts.get());
    }

    @Test
    void clearingCacheCancelsQueuedWorkWithoutReleasingTheNewClaim() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicInteger loads = new AtomicInteger();
        FileTextPlaceholder placeholder = new FileTextPlaceholder(tasks::add, source -> List.of("value-" + loads.incrementAndGet()), () -> 0L);

        placeholder.getCachedOrLoadAsync("config/value.txt");
        Runnable cancelledTask = tasks.removeNext();
        placeholder.clearContentCache();
        assertFalse(placeholder.isSourceLoading("config/value.txt"));
        placeholder.getCachedOrLoadAsync("config/value.txt");
        Runnable currentTask = tasks.removeNext();

        cancelledTask.run();
        assertEquals(0, loads.get());
        assertTrue(placeholder.isSourceLoading("config/value.txt"));
        currentTask.run();
        assertEquals(1, loads.get());
        assertEquals(List.of("value-1"), placeholder.getCachedOrLoadAsync("config/value.txt"));
    }

    @Test
    void launcherRejectionReleasesClaimForImmediateRetry() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicBoolean reject = new AtomicBoolean(true);
        FileTextPlaceholder placeholder = new FileTextPlaceholder(task -> {
            if (reject.getAndSet(false)) throw new RejectedExecutionException("expected test rejection");
            tasks.add(task);
        }, source -> List.of("loaded"), () -> 0L);

        assertNull(placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertFalse(placeholder.isSourceLoading("config/value.txt"));
        assertNull(placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertEquals(1, tasks.size());
        tasks.runNext();
        assertEquals(List.of("loaded"), placeholder.getCachedOrLoadAsync("config/value.txt"));
    }

    @Test
    void fatalLauncherErrorReleasesClaimBeforePropagation() {
        ManualTaskQueue tasks = new ManualTaskQueue();
        AtomicBoolean fail = new AtomicBoolean(true);
        FileTextPlaceholder placeholder = new FileTextPlaceholder(task -> {
            if (fail.getAndSet(false)) throw new AssertionError("expected test error");
            tasks.add(task);
        }, source -> List.of("loaded"), () -> 0L);

        assertThrows(AssertionError.class, () -> placeholder.getCachedOrLoadAsync("config/value.txt"));
        assertFalse(placeholder.isSourceLoading("config/value.txt"));
        assertNull(placeholder.getCachedOrLoadAsync("config/value.txt"));
        tasks.runNext();
        assertEquals(List.of("loaded"), placeholder.getCachedOrLoadAsync("config/value.txt"));
    }

    @Test
    void convertsDocumentedTextualNewlineSeparator() {
        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\n"));
    }

    @Test
    void preservesNormalAndEmptySeparators() {
        assertEquals("first | second", FileTextPlaceholder.joinLines(List.of("first", "second"), " | "));
        assertEquals("firstsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), ""));
    }

    @Test
    void returnsEmptyTextForEmptyInput() {
        assertEquals("", FileTextPlaceholder.joinLines(List.of(), "\\n"));
    }

    @Test
    void preservesActualLineEndings() {
        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\n"));
        assertEquals("first\r\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\r\n"));
    }

    @Test
    void decodesOnlyTextualNewlinesAndPreservesOtherBackslashes() {
        assertEquals("first\\r\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\r\\n"));
        assertEquals("first\\\nsecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "\\\\n"));
        assertEquals("firstC:\\temp\\foldersecond", FileTextPlaceholder.joinLines(List.of("first", "second"), "C:\\temp\\folder"));
    }

    @Test
    void resolvesSeparatorForEachUseWithoutMutatingCachedLines() {
        List<String> cachedLines = new ArrayList<>(List.of("first", "second"));

        assertEquals("first\nsecond", FileTextPlaceholder.joinLines(cachedLines, "\\n"));
        assertEquals("first, second", FileTextPlaceholder.joinLines(cachedLines, ", "));
        assertEquals(List.of("first", "second"), cachedLines);
    }

    @Test
    void readsRootAndNestedFilesFromBothDocumentedRoots() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Files.writeString(gameRoot.resolve("root.txt"), "game-root");
        Files.writeString(Files.createDirectories(gameRoot.resolve("config/nested")).resolve("value.txt"), "game-first\ngame-second");
        Files.writeString(Files.createDirectories(minecraftRoot.resolve("config/nested")).resolve("value.txt"), "minecraft-value");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        assertAll(
                () -> assertEquals(List.of("game-root"), FileTextPlaceholder.loadFromFile("root.txt", resolver)),
                () -> assertEquals(List.of("game-first", "game-second"), FileTextPlaceholder.loadFromFile("[source:local]/config/nested/value.txt", resolver)),
                () -> assertEquals(List.of("minecraft-value"), FileTextPlaceholder.loadFromFile(".minecraft/config/nested/value.txt", resolver)),
                () -> assertTrue(FileTextPlaceholder.isUrl("https://example.com/data.txt")),
                () -> assertTrue(FileTextPlaceholder.isUrl("http://example.com/data.txt")));
    }

    @Test
    void returnsNoContentForMissingMalformedAndNonLocalSources() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        assertAll(
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("missing.txt", resolver)),
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("bad\0path", resolver)),
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("[source:location]fancymenu:texts/example.txt", resolver)),
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("[source:web]https://example.com/data.txt", resolver)),
                () -> assertFalse(FileTextPlaceholder.isUrl("config/data.txt")));
    }

    @Test
    void rejectsTraversalSiblingDriveAndUncSourcesWithoutPublishingOutsideText() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path sibling = Files.createDirectory(this.temporaryDirectory.resolve("game-backup"));
        Path outsideFile = Files.writeString(sibling.resolve("secret.txt"), "outside-secret");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        List<List<String>> rejectedResults = List.of(
                FileTextPlaceholder.loadFromFile("../game-backup/secret.txt", resolver),
                FileTextPlaceholder.loadFromFile("nested\\..\\..\\game-backup\\secret.txt", resolver),
                FileTextPlaceholder.loadFromFile(outsideFile.toString(), resolver),
                FileTextPlaceholder.loadFromFile("C:\\outside\\secret.txt", resolver),
                FileTextPlaceholder.loadFromFile("//server/share/secret.txt", resolver));

        assertTrue(rejectedResults.stream().allMatch(List::isEmpty));
        assertTrue(rejectedResults.stream().flatMap(List::stream).noneMatch("outside-secret"::equals));
    }

    @Test
    void rejectsEscapingAndDanglingLinksWhileReadingInternalLinks() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "outside-secret");
        Path internal = Files.createDirectory(gameRoot.resolve("internal"));
        Path internalFile = Files.writeString(internal.resolve("value.txt"), "inside-value");
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-file.txt"), outsideFile);
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-directory"), outside);
        createSymbolicLinkOrSkip(gameRoot.resolve("dangling.txt"), outside.resolve("missing.txt"));
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-directory"), internal);
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-file.txt"), internalFile);
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        assertAll(
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("escaping-file.txt", resolver)),
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("escaping-directory/secret.txt", resolver)),
                () -> assertEquals(List.of(), FileTextPlaceholder.loadFromFile("dangling.txt", resolver)),
                () -> assertEquals(List.of("inside-value"), FileTextPlaceholder.loadFromFile("internal-directory/value.txt", resolver)),
                () -> assertEquals(List.of("inside-value"), FileTextPlaceholder.loadFromFile("internal-file.txt", resolver)));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }

}
