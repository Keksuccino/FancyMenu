package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MCEFVideoSourceResolverTest {

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
                () -> assertEquals(gameVideo.toUri().toString(), MCEFVideoSourceResolver.resolve(rawLocal, "videos/nested/video.mp4", resolver)),
                () -> assertEquals(minecraftVideo.toUri().toString(), MCEFVideoSourceResolver.resolve(rawLocal, ".minecraft/videos/video.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "missing.mp4", resolver)));
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
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "../game-backup/secret.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "nested\\..\\..\\game-backup\\secret.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, outsideVideo.toString(), resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "https://example.com/secret.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "bad\0path", resolver)));
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
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "escaping-file.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "escaping-directory/secret.mp4", resolver)),
                () -> assertNull(MCEFVideoSourceResolver.resolve(rawLocal, "dangling.mp4", resolver)),
                () -> assertEquals(gameRoot.resolve("internal-file.mp4").toUri().toString(), MCEFVideoSourceResolver.resolve(rawLocal, "internal-file.mp4", resolver)));
    }

    @Test
    void leavesRemoteAndResourcePackSourcesUnchanged() {
        ResourceSource web = ResourceSource.of("[source:web]https://example.com/video.mp4");
        ResourceSource location = ResourceSource.of("[source:location]fancymenu:videos/example.mp4");

        assertAll(
                () -> assertEquals("https://example.com/video.mp4?value=../unchanged", MCEFVideoSourceResolver.resolve(web, "https://example.com/video.mp4?value=../unchanged")),
                () -> assertEquals("fancymenu:videos/example.mp4", MCEFVideoSourceResolver.resolve(location, "fancymenu:videos/example.mp4")));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
