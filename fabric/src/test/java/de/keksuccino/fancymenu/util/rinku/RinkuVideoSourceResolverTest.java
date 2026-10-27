package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RinkuVideoSourceResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void revalidatesPlaceholderExpandedLocalSourcesAtTheChromiumHandoff() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path gameVideo = Files.writeString(Files.createDirectories(gameRoot.resolve("videos/nested")).resolve("video.mp4"), "video");
        Path minecraftVideo = Files.writeString(Files.createDirectories(minecraftRoot.resolve("videos")).resolve("video.mp4"), "video");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        ResourceSource rawLocal = ResourceSource.of("safe-placeholder.mp4", ResourceSourceType.LOCAL);

        assertAll(
                () -> assertEquals(gameVideo.toUri().toString(), RinkuVideoSourceResolver.resolve(rawLocal, "videos/nested/video.mp4", resolver)),
                () -> assertEquals(minecraftVideo.toUri().toString(), RinkuVideoSourceResolver.resolve(rawLocal, ".minecraft/videos/video.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "missing.mp4", resolver)));
    }

    @Test
    void neverPassesRejectedLocalPlaceholderOutputThroughAsAUrl() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path sibling = Files.createDirectory(this.temporaryDirectory.resolve("game-backup"));
        Path outsideVideo = Files.writeString(sibling.resolve("secret.mp4"), "outside-secret");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        ResourceSource rawLocal = ResourceSource.of("safe-placeholder.mp4", ResourceSourceType.LOCAL);

        assertAll(
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "../game-backup/secret.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "nested\\..\\..\\game-backup\\secret.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, outsideVideo.toString(), resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "https://example.com/secret.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "bad\0path", resolver)));
    }

    @Test
    void rejectsEscapingLinksAndPermitsInternalLinks() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideVideo = Files.writeString(outside.resolve("secret.mp4"), "outside-secret");
        Path internalVideo = Files.writeString(Files.createDirectory(gameRoot.resolve("internal")).resolve("video.mp4"), "inside-video");
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-file.mp4"), outsideVideo);
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-directory"), outside);
        createSymbolicLinkOrSkip(gameRoot.resolve("dangling.mp4"), outside.resolve("missing.mp4"));
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-file.mp4"), internalVideo);
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        ResourceSource rawLocal = ResourceSource.of("safe-placeholder.mp4", ResourceSourceType.LOCAL);

        assertAll(
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "escaping-file.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "escaping-directory/secret.mp4", resolver)),
                () -> assertNull(RinkuVideoSourceResolver.resolve(rawLocal, "dangling.mp4", resolver)),
                () -> assertEquals(gameRoot.resolve("internal-file.mp4").toUri().toString(), RinkuVideoSourceResolver.resolve(rawLocal, "internal-file.mp4", resolver)));
    }

    @Test
    void leavesRemoteAndResourcePackSourcesUnchanged() {
        ResourceSource web = ResourceSource.of("[source:web]https://example.com/video.mp4");
        ResourceSource location = ResourceSource.of("[source:location]fancymenu:videos/example.mp4");

        assertAll(
                () -> assertEquals("https://example.com/video.mp4?value=../unchanged", RinkuVideoSourceResolver.resolve(web, "https://example.com/video.mp4?value=../unchanged")),
                () -> assertEquals("fancymenu:videos/example.mp4", RinkuVideoSourceResolver.resolve(location, "fancymenu:videos/example.mp4")));
    }

    @Test
    void cachedLocalChecksOnlyTouchTheFileSystemAgainAfterTheRecheckIntervalOrASourceChange() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger checks = new AtomicInteger();
        RinkuVideoSourceResolver resolver = new RinkuVideoSourceResolver(clock::get, (raw, expanded) -> {
            checks.incrementAndGet();
            return "file:///" + expanded;
        });
        ResourceSource rawLocal = ResourceSource.of("video.mp4", ResourceSourceType.LOCAL);

        assertEquals("file:///first.mp4", resolver.resolveCached(rawLocal, "first.mp4"));
        clock.addAndGet(RinkuVideoSourceResolver.LOCAL_RECHECK_INTERVAL_NANOS - 1L);
        assertEquals("file:///first.mp4", resolver.resolveCached(rawLocal, "first.mp4"));
        assertEquals(1, checks.get());

        clock.incrementAndGet();
        assertEquals("file:///first.mp4", resolver.resolveCached(rawLocal, "first.mp4"));
        assertEquals(2, checks.get());

        assertEquals("file:///second.mp4", resolver.resolveCached(rawLocal, "second.mp4"));
        assertEquals(3, checks.get());

        resolver.reset();
        assertEquals("file:///second.mp4", resolver.resolveCached(rawLocal, "second.mp4"));
        assertEquals(4, checks.get());
    }

    @Test
    void noticesLocalFilesThatAppearAfterTheFirstCheck() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver pathResolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        AtomicLong clock = new AtomicLong();
        RinkuVideoSourceResolver resolver = new RinkuVideoSourceResolver(clock::get, (raw, expanded) -> RinkuVideoSourceResolver.resolve(raw, expanded, pathResolver));
        ResourceSource rawLocal = ResourceSource.of("later.mp4", ResourceSourceType.LOCAL);

        assertNull(resolver.resolveCached(rawLocal, "later.mp4"));
        Path video = Files.writeString(gameRoot.resolve("later.mp4"), "video");
        assertNull(resolver.resolveCached(rawLocal, "later.mp4"));
        clock.addAndGet(RinkuVideoSourceResolver.LOCAL_RECHECK_INTERVAL_NANOS);
        assertEquals(video.toUri().toString(), resolver.resolveCached(rawLocal, "later.mp4"));
    }

    @Test
    void nonLocalSourcesSkipTheFileSystemAndDropTheLastLocalCheck() {
        AtomicInteger checks = new AtomicInteger();
        RinkuVideoSourceResolver resolver = new RinkuVideoSourceResolver(() -> 0L, (raw, expanded) -> {
            checks.incrementAndGet();
            return "file:///" + expanded;
        });
        ResourceSource rawLocal = ResourceSource.of("video.mp4", ResourceSourceType.LOCAL);
        ResourceSource web = ResourceSource.of("[source:web]https://example.com/video.mp4");

        resolver.resolveCached(rawLocal, "video.mp4");
        assertEquals("https://example.com/video.mp4?a=1&embed=true", resolver.resolveCached(web, "https://example.com/video.mp4?a=1&embed=true"));
        assertEquals(1, checks.get());
        resolver.resolveCached(rawLocal, "video.mp4");
        assertEquals(2, checks.get());
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
