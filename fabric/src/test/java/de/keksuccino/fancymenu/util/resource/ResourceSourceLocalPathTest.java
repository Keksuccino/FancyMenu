package de.keksuccino.fancymenu.util.resource;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResourceSourceLocalPathTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesAndSerializesValidSourcesInBothDocumentedRoots() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path gameFile = Files.writeString(Files.createDirectories(gameRoot.resolve("config/fancymenu/assets/nested")).resolve("value.txt"), "game-value");
        Path minecraftFile = Files.writeString(Files.createDirectories(minecraftRoot.resolve("config/nested")).resolve("value.txt"), "minecraft-value");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        ResourceSource relative = ResourceSource.of("[source:local]config/fancymenu/assets/nested/value.txt", null, resolver);
        ResourceSource virtualRoot = ResourceSource.of("/config/fancymenu/assets/nested/value.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource minecraft = ResourceSource.of(".minecraft/config/nested/value.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource absoluteMinecraft = ResourceSource.of(minecraftFile.toString(), ResourceSourceType.LOCAL, resolver);

        assertAll(
                () -> assertEquals(gameFile.toFile(), relative.getValidatedLocalFile()),
                () -> assertEquals(gameFile.toFile(), virtualRoot.getValidatedLocalFile()),
                () -> assertEquals("[source:local]config/fancymenu/assets/nested/value.txt", relative.getSerializationSource()),
                () -> assertEquals("[source:local].minecraft/config/nested/value.txt", minecraft.getSerializationSource()),
                () -> assertEquals(minecraftFile.toFile(), minecraft.getValidatedLocalFile()),
                () -> assertEquals(minecraftFile.toFile(), absoluteMinecraft.getValidatedLocalFile()),
                () -> assertTrue(minecraft.isDotMinecraftSource()));
    }

    @Test
    void safelyRepresentsRejectedTraversalSiblingDriveAndMalformedSources() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        String sibling = this.temporaryDirectory.resolve("game-backup/secret.txt").toAbsolutePath().toString();

        ResourceSource traversal = ResourceSource.of("../secret.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource mixedTraversal = ResourceSource.of("nested\\..\\secret.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource siblingPrefix = ResourceSource.of(sibling, ResourceSourceType.LOCAL, resolver);
        ResourceSource drive = ResourceSource.of("C:\\outside\\secret.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource malformed = ResourceSource.of("bad\0path", ResourceSourceType.LOCAL, resolver);

        assertAll(
                () -> assertRejectedButSerializable(traversal, "../secret.txt"),
                () -> assertRejectedButSerializable(mixedTraversal, "nested\\..\\secret.txt"),
                () -> assertRejectedButSerializable(siblingPrefix, sibling),
                () -> assertRejectedButSerializable(drive, "C:\\outside\\secret.txt"),
                () -> assertRejectedButSerializable(malformed, "bad\0path"));
    }

    @Test
    void rebasesSingleSlashVirtualSyntaxWithoutPublishingSiblingData() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outsideFile = Files.writeString(Files.createDirectory(this.temporaryDirectory.resolve("game-backup")).resolve("secret.txt"), "outside-secret");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        ResourceSource virtualSource = ResourceSource.of("/legacy/secret.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource siblingSource = ResourceSource.of(outsideFile.toString(), ResourceSourceType.LOCAL, resolver);
        File validatedVirtualFile = virtualSource.getValidatedLocalFile();

        assertAll(
                () -> assertEquals(gameRoot.resolve("legacy/secret.txt").toFile(), validatedVirtualFile),
                () -> assertFalse(outsideFile.toFile().equals(validatedVirtualFile)),
                () -> assertFalse((validatedVirtualFile != null) && validatedVirtualFile.isFile()),
                () -> assertNull(siblingSource.getValidatedLocalFile()));
    }

    @Test
    void permitsSafeNonexistentAndInternalLinksButRejectsEveryEscapingLinkForm() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "outside-secret");
        Path internal = Files.createDirectory(gameRoot.resolve("internal"));
        Path internalFile = Files.writeString(internal.resolve("value.txt"), "inside");
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-file.txt"), outsideFile);
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-ancestor"), outside);
        createSymbolicLinkOrSkip(gameRoot.resolve("dangling.txt"), outside.resolve("missing.txt"));
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-link"), internal);
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-file.txt"), internalFile);
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        ResourceSource nonexistent = ResourceSource.of("future/deep/value.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource escapingFinal = ResourceSource.of("escaping-file.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource escapingAncestor = ResourceSource.of("escaping-ancestor/value.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource dangling = ResourceSource.of("dangling.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource internalDirectory = ResourceSource.of("internal-link/value.txt", ResourceSourceType.LOCAL, resolver);
        ResourceSource internalFinal = ResourceSource.of("internal-file.txt", ResourceSourceType.LOCAL, resolver);

        assertAll(
                () -> assertEquals(gameRoot.resolve("future/deep/value.txt").toFile(), nonexistent.getValidatedLocalFile()),
                () -> assertNull(escapingFinal.getValidatedLocalFile()),
                () -> assertNull(escapingAncestor.getValidatedLocalFile()),
                () -> assertNull(dangling.getValidatedLocalFile()),
                () -> assertEquals(gameRoot.resolve("internal-link/value.txt").toFile(), internalDirectory.getValidatedLocalFile()),
                () -> assertEquals(gameRoot.resolve("internal-file.txt").toFile(), internalFinal.getValidatedLocalFile()));
    }

    @Test
    void revalidationPreventsANewEscapingAncestorFromReachingDirectCodecs() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Files.writeString(outside.resolve("secret.txt"), "outside-secret");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        ResourceSource source = ResourceSource.of("future/secret.txt", ResourceSourceType.LOCAL, resolver);
        FileCodec<String> codec = FileCodec.basicWithLocal(String.class, stream -> "stream", location -> "location", ResourceSourceLocalPathTest::readFile);

        createSymbolicLinkOrSkip(gameRoot.resolve("future"), outside);

        assertAll(
                () -> assertNull(source.getValidatedLocalFile()),
                () -> assertFalse(FileTypes.TXT_TEXT.isFileType(source, false)),
                () -> assertNull(codec.read(source)));
    }

    @Test
    void leavesWebAndResourcePackSourcesUnchanged() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        ResourceSource web = ResourceSource.of("[source:web]https://example.com/media/video.mp4?x=../secret", null, resolver);
        ResourceSource location = ResourceSource.of("[source:location]fancymenu:textures/gui/background.png", null, resolver);

        assertAll(
                () -> assertEquals(ResourceSourceType.WEB, web.getSourceType()),
                () -> assertEquals("https://example.com/media/video.mp4?x=../secret", web.getSourceWithoutPrefix()),
                () -> assertEquals("[source:web]https://example.com/media/video.mp4?x=../secret", web.getSerializationSource()),
                () -> assertNull(web.getValidatedLocalFile()),
                () -> assertEquals(ResourceSourceType.LOCATION, location.getSourceType()),
                () -> assertEquals("fancymenu:textures/gui/background.png", location.getSourceWithoutPrefix()),
                () -> assertEquals("[source:location]fancymenu:textures/gui/background.png", location.getSerializationSource()),
                () -> assertNull(location.getValidatedLocalFile()));
    }

    private static void assertRejectedButSerializable(ResourceSource source, String serializedPayload) {
        assertAll(
                () -> assertNull(source.getValidatedLocalFile()),
                () -> assertEquals("", source.getSourceWithoutPrefix()),
                () -> assertEquals("[source:local]" + serializedPayload, source.getSerializationSource()),
                () -> assertEquals("[source:local]" + serializedPayload, source.getSourceWithPrefix()));
    }

    private static String readFile(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
