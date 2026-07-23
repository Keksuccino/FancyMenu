package de.keksuccino.fancymenu.customization.action.actions.file;

import com.sun.net.httpserver.HttpServer;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolverFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GameDirectoryFileActionsTest {

    @TempDir
    Path temporaryDirectory;

    private Path gameRoot;
    private Path minecraftRoot;
    private Path outsideRoot;
    private GameDirectoryActionPathResolver resolver;
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        this.gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        this.minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        this.outsideRoot = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        this.resolver = GameDirectoryActionPathResolverFixture.create(this.gameRoot, this.minecraftRoot);
    }

    @AfterEach
    void cleanUpServer() {
        if (this.server != null) {
            this.server.stop(0);
        }
    }

    @Test
    void createRejectsTraversalSymlinkEscapesAndRootSelectionWithoutSideEffects() throws Exception {
        Path escapingDirectory = this.gameRoot.resolve("escaping");
        createSymbolicLinkOrSkip(escapingDirectory, this.outsideRoot);
        CreateFileAction action = new CreateFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/created.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("escaping/created.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/", this.resolver)),
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("created.txt"))));

        action.executeWithResolver("config/deep/created.txt", this.resolver);
        action.executeWithResolver(".minecraft/config/created.txt", this.resolver);

        assertAll(
                () -> assertTrue(Files.isRegularFile(this.gameRoot.resolve("config/deep/created.txt"))),
                () -> assertTrue(Files.isRegularFile(this.minecraftRoot.resolve("config/created.txt"))));
    }

    @Test
    void writeRejectsTraversalAndSymlinkEscapesAndPreservesValidOverwriteAndAppendBehavior() throws Exception {
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "original");
        Path escapingFile = this.gameRoot.resolve("escaping.txt");
        createSymbolicLinkOrSkip(escapingFile, outsideFile);
        WriteFileAction action = new WriteFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/outside.txt|||changed|||false", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("escaping.txt|||changed|||false", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".|||changed|||false", this.resolver)),
                () -> assertEquals("original", Files.readString(outsideFile)));

        action.executeWithResolver("config/output.txt|||first\\nline|||false", this.resolver);
        action.executeWithResolver("config/output.txt|||\\nsecond|||true", this.resolver);

        assertEquals("first\nline\nsecond", Files.readString(this.gameRoot.resolve("config/output.txt")));
    }

    @Test
    void copyRejectsBothEndpointTraversalAndLeavesOutsideFilesUntouched() throws Exception {
        Path source = Files.writeString(this.gameRoot.resolve("source.txt"), "inside");
        Path outsideSource = Files.writeString(this.outsideRoot.resolve("source.txt"), "outside");
        CopyFileAction action = new CopyFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/source.txt||copied.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||../outside/copied.txt", this.resolver)),
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("copied.txt"))),
                () -> assertEquals("inside", Files.readString(source)),
                () -> assertEquals("outside", Files.readString(outsideSource)));

        action.executeWithResolver("source.txt||nested/copied.txt", this.resolver);

        assertEquals("inside", Files.readString(this.gameRoot.resolve("nested/copied.txt")));
    }

    @Test
    void recursiveCopyPreflightsEscapingNestedSymlinksBeforeCreatingDestination() throws Exception {
        Path sourceDirectory = Files.createDirectories(this.gameRoot.resolve("source/nested"));
        Files.writeString(sourceDirectory.resolve("ordinary.txt"), "ordinary");
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("secret.txt"), "secret");
        createSymbolicLinkOrSkip(sourceDirectory.resolve("escaping.txt"), outsideFile);
        CopyFileAction action = new CopyFileAction();

        assertThrows(SecurityException.class, () -> action.executeWithResolver("source||destination", this.resolver));

        assertFalse(Files.exists(this.gameRoot.resolve("destination")), "Preflight must finish before creating any destination entries");
        assertEquals("secret", Files.readString(outsideFile));
    }

    @Test
    void wildcardCopyPreflightsAllEntriesAndSupportsOrdinaryFiles() throws Exception {
        Path sourceDirectory = Files.createDirectory(this.gameRoot.resolve("source"));
        Files.writeString(sourceDirectory.resolve("first.txt"), "first");
        Files.writeString(sourceDirectory.resolve("second.txt"), "second");
        Files.createDirectory(sourceDirectory.resolve("ignored-directory"));
        CopyFileAction action = new CopyFileAction();

        action.executeWithResolver("source/*||destination", this.resolver);

        assertAll(
                () -> assertEquals("first", Files.readString(this.gameRoot.resolve("destination/first.txt"))),
                () -> assertEquals("second", Files.readString(this.gameRoot.resolve("destination/second.txt"))),
                () -> assertFalse(Files.exists(this.gameRoot.resolve("destination/ignored-directory"))));
    }

    @Test
    void wildcardCopyMayUseTheGameRootAsASafeChildContainer() throws Exception {
        Path sourceDirectory = Files.createDirectory(this.gameRoot.resolve("copy-source"));
        Files.writeString(sourceDirectory.resolve("copied-to-root.txt"), "copied");

        new CopyFileAction().executeWithResolver("copy-source/*||/", this.resolver);

        assertEquals("copied", Files.readString(this.gameRoot.resolve("copied-to-root.txt")));
    }

    @Test
    void moveRejectsBothEndpointTraversalAndAllowedRootSources() throws Exception {
        Path source = Files.writeString(this.gameRoot.resolve("source.txt"), "inside");
        Path outsideSource = Files.writeString(this.outsideRoot.resolve("source.txt"), "outside");
        MoveFileAction action = new MoveFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/source.txt||moved.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||../outside/moved.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/||moved-root", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".minecraft||moved-minecraft-root", this.resolver)),
                () -> assertTrue(Files.isRegularFile(source)),
                () -> assertEquals("outside", Files.readString(outsideSource)),
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("moved.txt"))));

        action.executeWithResolver("source.txt||nested/moved.txt", this.resolver);

        assertAll(
                () -> assertFalse(Files.exists(source)),
                () -> assertEquals("inside", Files.readString(this.gameRoot.resolve("nested/moved.txt"))));
    }

    @Test
    void wildcardMovePreflightsSymlinkEscapesAndConflictsBeforeMovingAnything() throws Exception {
        Path sourceDirectory = Files.createDirectory(this.gameRoot.resolve("source"));
        Path first = Files.writeString(sourceDirectory.resolve("first.txt"), "first");
        Path second = Files.writeString(sourceDirectory.resolve("second.txt"), "second");
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        createSymbolicLinkOrSkip(sourceDirectory.resolve("escaping.txt"), outsideFile);
        MoveFileAction action = new MoveFileAction();

        assertThrows(SecurityException.class, () -> action.executeWithResolver("source/*||destination", this.resolver));

        assertAll(
                () -> assertTrue(Files.isRegularFile(first)),
                () -> assertTrue(Files.isRegularFile(second)),
                () -> assertFalse(Files.exists(this.gameRoot.resolve("destination"))),
                () -> assertEquals("outside", Files.readString(outsideFile)));

        Files.delete(sourceDirectory.resolve("escaping.txt"));
        Path conflictDirectory = Files.createDirectory(this.gameRoot.resolve("conflict-destination"));
        Files.writeString(conflictDirectory.resolve("second.txt"), "conflict");

        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> action.executeWithResolver("source/*||conflict-destination", this.resolver));
        assertAll(() -> assertTrue(Files.isRegularFile(first)), () -> assertTrue(Files.isRegularFile(second)));

        action.executeWithResolver("source/*||destination", this.resolver);
        assertAll(
                () -> assertFalse(Files.exists(first)),
                () -> assertFalse(Files.exists(second)),
                () -> assertEquals("first", Files.readString(this.gameRoot.resolve("destination/first.txt"))),
                () -> assertEquals("second", Files.readString(this.gameRoot.resolve("destination/second.txt"))));
    }

    @Test
    void wildcardMoveMayUseTheGameRootAsASafeChildContainer() throws Exception {
        Path sourceDirectory = Files.createDirectory(this.gameRoot.resolve("move-source"));
        Path source = Files.writeString(sourceDirectory.resolve("moved-to-root.txt"), "moved");

        new MoveFileAction().executeWithResolver("move-source/*||/", this.resolver);

        assertAll(
                () -> assertFalse(Files.exists(source)),
                () -> assertEquals("moved", Files.readString(this.gameRoot.resolve("moved-to-root.txt"))));
    }

    @Test
    void deleteRejectsTraversalAndEveryAllowedRootSpelling() throws Exception {
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path insideFile = Files.writeString(this.gameRoot.resolve("inside.txt"), "inside");
        DeleteFileAction action = new DeleteFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/outside.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("directory/..", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/*", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".minecraft", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".minecraft/*", this.resolver)),
                () -> assertEquals("outside", Files.readString(outsideFile)),
                () -> assertTrue(Files.isRegularFile(insideFile)));

        action.executeWithResolver("inside.txt", this.resolver);

        assertFalse(Files.exists(insideFile));
    }

    @Test
    void recursiveDeletePreflightsNestedSymlinkEscapesBeforeDeletingAnything() throws Exception {
        Path targetDirectory = Files.createDirectories(this.gameRoot.resolve("target/nested"));
        Path ordinaryFile = Files.writeString(targetDirectory.resolve("ordinary.txt"), "ordinary");
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path escapingLink = targetDirectory.resolve("escaping.txt");
        createSymbolicLinkOrSkip(escapingLink, outsideFile);
        DeleteFileAction action = new DeleteFileAction();

        assertThrows(SecurityException.class, () -> action.executeWithResolver("target", this.resolver));

        assertAll(
                () -> assertTrue(Files.isRegularFile(ordinaryFile)),
                () -> assertTrue(Files.isSymbolicLink(escapingLink)),
                () -> assertEquals("outside", Files.readString(outsideFile)),
                () -> assertTrue(Files.isDirectory(this.gameRoot.resolve("target"))));

        Files.delete(escapingLink);
        action.executeWithResolver("target", this.resolver);
        assertFalse(Files.exists(this.gameRoot.resolve("target")));
    }

    @Test
    void wildcardDeletePreflightsAllEntriesAndLeavesDirectoriesIntact() throws Exception {
        Path targetDirectory = Files.createDirectory(this.gameRoot.resolve("target"));
        Path first = Files.writeString(targetDirectory.resolve("first.txt"), "first");
        Path second = Files.writeString(targetDirectory.resolve("second.txt"), "second");
        Path retainedDirectory = Files.createDirectory(targetDirectory.resolve("retained"));
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path escapingLink = targetDirectory.resolve("escaping.txt");
        createSymbolicLinkOrSkip(escapingLink, outsideFile);
        DeleteFileAction action = new DeleteFileAction();

        assertThrows(SecurityException.class, () -> action.executeWithResolver("target/*", this.resolver));
        assertAll(() -> assertTrue(Files.isRegularFile(first)), () -> assertTrue(Files.isRegularFile(second)));

        Files.delete(escapingLink);
        action.executeWithResolver("target/*", this.resolver);

        assertAll(
                () -> assertFalse(Files.exists(first)),
                () -> assertFalse(Files.exists(second)),
                () -> assertTrue(Files.isDirectory(retainedDirectory)),
                () -> assertEquals("outside", Files.readString(outsideFile)));
    }

    @Test
    void downloadRejectsTargetTraversalBeforeContactingTheServer() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        this.server = startServer("/file.txt", exchange -> {
            requestCount.incrementAndGet();
            byte[] body = "download".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        String url = serverUrl("/file.txt");

        new DownloadFileAction().executeWithResolver(url + "||../outside", this.resolver).get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertEquals(0, requestCount.get()),
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("file.txt"))));
    }

    @Test
    void downloadRevalidatesTheDestinationAfterTheNetworkPhase() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        CountDownLatch sendResponse = new CountDownLatch(1);
        this.server = startServer("/delayed.txt", exchange -> {
            try {
                requestReceived.countDown();
                if (!sendResponse.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out waiting for destination swap");
                }
                byte[] body = "download".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ex);
            } finally {
                exchange.close();
            }
        });
        DownloadFileAction action = new DownloadFileAction();
        var completion = action.executeWithResolver(serverUrl("/delayed.txt") + "||downloads", this.resolver);
        assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "The loopback request was not received");
        Path targetDirectory = this.gameRoot.resolve("downloads");
        assertTrue(Files.isDirectory(targetDirectory));
        Files.delete(targetDirectory);
        createSymbolicLinkOrSkip(targetDirectory, this.outsideRoot);
        sendResponse.countDown();

        completion.get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("delayed.txt"))),
                () -> assertTrue(Files.isSymbolicLink(targetDirectory)));
    }

    @Test
    void downloadRejectsAnEscapingFinalSymlinkAndAcceptsASanitizedSingleFilename() throws Exception {
        this.server = startServer("/payload", exchange -> {
            byte[] body = "download".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=../downloaded.txt");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        Path downloadDirectory = Files.createDirectory(this.gameRoot.resolve("downloads"));
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path targetLink = downloadDirectory.resolve("downloaded.txt");
        createSymbolicLinkOrSkip(targetLink, outsideFile);
        DownloadFileAction action = new DownloadFileAction();

        action.executeWithResolver(serverUrl("/payload") + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);

        assertEquals("outside", Files.readString(outsideFile));
        Files.delete(targetLink);
        action.executeWithResolver(serverUrl("/payload") + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);

        assertEquals("download", Files.readString(downloadDirectory.resolve("downloaded.txt")));
    }

    @Test
    void downloadMayUseTheGameRootAsASafeChildContainer() throws Exception {
        this.server = startServer("/root-download.txt", exchange -> {
            byte[] body = "root-download".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        new DownloadFileAction().executeWithResolver(serverUrl("/root-download.txt") + "||/", this.resolver).get(5, TimeUnit.SECONDS);

        assertEquals("root-download", Files.readString(this.gameRoot.resolve("root-download.txt")));
    }

    @Test
    void downloadCleanupSkipsAPathWhoseParentWasSwappedOutsideTheRoot() throws Exception {
        Path downloadDirectory = Files.createDirectory(this.gameRoot.resolve("downloads"));
        Path temporaryFile = Files.writeString(downloadDirectory.resolve("temporary.tmp"), "temporary");
        GameDirectoryActionPathResolver.ResolvedPath temporaryHandle = this.resolver.resolve("downloads/temporary.tmp");
        Files.delete(temporaryFile);
        Files.delete(downloadDirectory);
        Path outsideSameName = Files.writeString(this.outsideRoot.resolve("temporary.tmp"), "outside");
        createSymbolicLinkOrSkip(downloadDirectory, this.outsideRoot);

        boolean deleted = new DownloadFileAction().cleanupTemporaryFile(temporaryHandle);

        assertAll(
                () -> assertFalse(deleted),
                () -> assertEquals("outside", Files.readString(outsideSameName)));
    }

    @Test
    void extractZipRejectsTraversalAtBothConfiguredEndpoints() throws Exception {
        Path sourceZip = createZip(this.gameRoot.resolve("archive.zip"), Map.of("file.txt", "inside"));
        Path outsideZip = createZip(this.outsideRoot.resolve("outside.zip"), Map.of("file.txt", "outside"));
        ExtractZipFileAction action = new ExtractZipFileAction();

        action.executeWithResolver("../outside/outside.zip||extracted-source", this.resolver).get(5, TimeUnit.SECONDS);
        action.executeWithResolver("archive.zip||../outside/extracted-target", this.resolver).get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertTrue(Files.isRegularFile(sourceZip)),
                () -> assertTrue(Files.isRegularFile(outsideZip)),
                () -> assertFalse(Files.exists(this.gameRoot.resolve("extracted-source"))),
                () -> assertFalse(Files.exists(this.outsideRoot.resolve("extracted-target"))));
    }

    @Test
    void extractZipPreflightsAllEntriesAndExistingSymlinksBeforeWritingAnything() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("ordinary.txt", "ordinary");
        entries.put("../escaped.txt", "escaped");
        Path traversalZip = createZip(this.gameRoot.resolve("traversal.zip"), entries);
        ExtractZipFileAction action = new ExtractZipFileAction();

        action.executeWithResolver("traversal.zip||extracted", this.resolver).get(5, TimeUnit.SECONDS);

        assertFalse(Files.exists(this.gameRoot.resolve("extracted")), "ZIP-slip detection must finish before creating destination entries");
        assertFalse(Files.exists(this.gameRoot.resolve("escaped.txt")));

        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path targetDirectory = Files.createDirectory(this.gameRoot.resolve("existing-target"));
        Path targetLink = targetDirectory.resolve("ordinary.txt");
        createSymbolicLinkOrSkip(targetLink, outsideFile);
        action.executeWithResolver(traversalZip.getFileName() + "||existing-target", this.resolver).get(5, TimeUnit.SECONDS);

        assertEquals("outside", Files.readString(outsideFile));
    }

    @Test
    void extractZipSupportsSafeNonExistingDescendantsAndTheGameRootContainer() throws Exception {
        createZip(this.gameRoot.resolve("archive.zip"), Map.of("nested/file.txt", "inside"));
        createZip(this.gameRoot.resolve("root-archive.zip"), Map.of("root-extracted.txt", "root"));
        ExtractZipFileAction action = new ExtractZipFileAction();

        action.executeWithResolver("archive.zip||new/deep/extracted", this.resolver).get(5, TimeUnit.SECONDS);
        action.executeWithResolver("root-archive.zip||/", this.resolver).get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertEquals("inside", Files.readString(this.gameRoot.resolve("new/deep/extracted/nested/file.txt"))),
                () -> assertEquals("root", Files.readString(this.gameRoot.resolve("root-extracted.txt"))));
    }

    @Test
    void selectFileCopyConfinesItsDestinationWhileAllowingAnExternalSelectedSource() throws Exception {
        Path selectedSource = Files.writeString(this.outsideRoot.resolve("selected.txt"), "selected");
        Path outsideTarget = this.outsideRoot.resolve("copied.txt");
        SelectFileAction action = new SelectFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.copySelectedFileWithResolver(selectedSource, "../outside/copied.txt", true, this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.copySelectedFileWithResolver(selectedSource, "/", true, this.resolver)),
                () -> assertFalse(Files.exists(outsideTarget)),
                () -> assertEquals("selected", Files.readString(selectedSource)));

        action.copySelectedFileWithResolver(selectedSource, "config/copied.txt", false, this.resolver);

        assertEquals("selected", Files.readString(this.gameRoot.resolve("config/copied.txt")));
    }

    @Test
    void selectFileCopyRejectsEscapingFinalSymlinksAndHonorsOverwriteMode() throws Exception {
        Path selectedSource = Files.writeString(this.outsideRoot.resolve("selected.txt"), "selected");
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path configDirectory = Files.createDirectory(this.gameRoot.resolve("config"));
        Path targetLink = configDirectory.resolve("linked.txt");
        createSymbolicLinkOrSkip(targetLink, outsideFile);
        SelectFileAction action = new SelectFileAction();

        assertThrows(SecurityException.class, () -> action.copySelectedFileWithResolver(selectedSource, "config/linked.txt", true, this.resolver));
        assertEquals("outside", Files.readString(outsideFile));

        Path target = Files.writeString(configDirectory.resolve("target.txt"), "original");
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> action.copySelectedFileWithResolver(selectedSource, "config/target.txt", false, this.resolver));
        assertEquals("original", Files.readString(target));

        action.copySelectedFileWithResolver(selectedSource, "config/target.txt", true, this.resolver);
        assertEquals("selected", Files.readString(target));
    }

    @Test
    void selectFileCopyTreatsAnOverwriteOntoTheSameFileAsANoOp() throws Exception {
        Path configDirectory = Files.createDirectory(this.gameRoot.resolve("config"));
        Path sameFile = Files.writeString(configDirectory.resolve("same.txt"), "preserved");
        SelectFileAction action = new SelectFileAction();

        action.copySelectedFileWithResolver(sameFile, "config/same.txt", true, this.resolver);

        assertEquals("preserved", Files.readString(sameFile));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> action.copySelectedFileWithResolver(sameFile, "config/same.txt", false, this.resolver));
        assertEquals("preserved", Files.readString(sameFile));
    }

    @Test
    void openFileFolderResolutionRejectsTraversalAndEscapingSymlinksWithoutInvokingTheOs() throws Exception {
        Path ordinaryFile = Files.writeString(this.gameRoot.resolve("ordinary.txt"), "inside");
        Path internalLink = this.gameRoot.resolve("internal-link.txt");
        createSymbolicLinkOrSkip(internalLink, ordinaryFile);
        Path outsideFile = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path escapingLink = this.gameRoot.resolve("escaping.txt");
        createSymbolicLinkOrSkip(escapingLink, outsideFile);
        OpenFileFolderAction action = new OpenFileFolderAction();
        GameDirectoryActionPathResolver.ResolvedPath internalTarget = action.resolveWithResolver("internal-link.txt", this.resolver);

        assertAll(
                () -> assertEquals(ordinaryFile, action.resolveWithResolver("ordinary.txt", this.resolver).path()),
                () -> assertEquals(ordinaryFile.toRealPath(), action.resolveRealPath(internalTarget)),
                () -> assertEquals(this.gameRoot, action.resolveWithResolver("/", this.resolver).path()),
                () -> assertThrows(SecurityException.class, () -> action.resolveWithResolver("../outside/outside.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.resolveWithResolver("escaping.txt", this.resolver)),
                () -> assertThrows(java.io.FileNotFoundException.class, () -> action.resolveWithResolver("missing.txt", this.resolver)));
    }

    @Test
    void renameRejectsTraversalSeparatorsDotNamesAndAllowedRoots() throws Exception {
        Path source = Files.writeString(this.gameRoot.resolve("source.txt"), "inside");
        Path outsideSource = Files.writeString(this.outsideRoot.resolve("outside.txt"), "outside");
        Path escapingSource = this.gameRoot.resolve("escaping-source.txt");
        createSymbolicLinkOrSkip(escapingSource, outsideSource);
        RenameFileAction action = new RenameFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/outside.txt||renamed.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("escaping-source.txt||renamed.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||../escaped.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||nested/escaped.txt", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||nested\\escaped.txt", this.resolver)),
                () -> assertThrows(IllegalArgumentException.class, () -> action.executeWithResolver("source.txt||", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||.", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("source.txt||..", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/||renamed-game", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".minecraft||renamed-minecraft", this.resolver)),
                () -> assertTrue(Files.isRegularFile(source)),
                () -> assertEquals("outside", Files.readString(outsideSource)));

        action.executeWithResolver("source.txt||renamed.txt", this.resolver);

        assertAll(
                () -> assertFalse(Files.exists(source)),
                () -> assertEquals("inside", Files.readString(this.gameRoot.resolve("renamed.txt"))));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }

    private HttpServer startServer(String path, com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext(path, handler);
        httpServer.start();
        return httpServer;
    }

    private String serverUrl(String path) {
        return "http://127.0.0.1:" + this.server.getAddress().getPort() + path;
    }

    private static Path createZip(Path path, Map<String, String> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return path;
    }
}
