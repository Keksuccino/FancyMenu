package de.keksuccino.fancymenu.util.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LocalSourcePathResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsDocumentedInstanceAndMinecraftSourceSyntax() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        LocalSourcePathResolver.ResolvedPath relative = resolver.resolve("config/fancymenu/assets/value.txt");
        LocalSourcePathResolver.ResolvedPath virtualRoot = resolver.resolve("/config/fancymenu/assets/value.txt");
        LocalSourcePathResolver.ResolvedPath minecraft = resolver.resolve(".minecraft/config/value.txt");

        assertAll(
                () -> assertEquals(gameRoot.resolve("config/fancymenu/assets/value.txt"), relative.path()),
                () -> assertEquals(relative.path(), virtualRoot.path()),
                () -> assertEquals(LocalSourcePathResolver.AllowedRoot.GAME_DIRECTORY, relative.allowedRoot()),
                () -> assertEquals(minecraftRoot.resolve("config/value.txt"), minecraft.path()),
                () -> assertEquals(LocalSourcePathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraft.allowedRoot()));
    }

    @Test
    void gameOnlyModeTreatsMinecraftAsALiteralInstanceChild() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        assertEquals(gameRoot.resolve(".minecraft/config/value.json"), resolver.resolve(".minecraft/config/value.json").path());
    }

    @Test
    void rejectsParentTraversalInBothSeparatorStylesBeforeNormalization() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("nested/../value.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("nested\\..\\value.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("/config/../../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft/../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft\\..\\outside.txt")));
    }

    @Test
    void rebasesSingleSlashVirtualRootsButRejectsSiblingPrefixesDriveAndUncForms() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameAndMinecraftDirectories(gameRoot, minecraftRoot);
        Path siblingPrefix = this.temporaryDirectory.resolve("game-backup/value.txt").toAbsolutePath();

        LocalSourcePathResolver.ResolvedPath safelyRebased = resolver.resolve("/legacy/value.txt");

        assertAll(
                () -> assertEquals(gameRoot.resolve("legacy/value.txt"), safelyRebased.path()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(siblingPrefix.toString())),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("C:\\outside\\value.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("C:outside\\value.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("//server/share/value.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("\\\\server\\share\\value.txt")));
    }

    @Test
    void acceptsContainedHostAbsoluteAndSafeNonexistentPathsAndRejectsMalformedValues() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);
        Path nested = Files.writeString(Files.createDirectories(gameRoot.resolve("nested")).resolve("value.txt"), "inside");

        assertAll(
                () -> assertEquals(nested, resolver.resolve(nested.toString()).path()),
                () -> assertEquals(gameRoot.resolve("future/deep/value.txt"), resolver.resolve("future/deep/value.txt").path()),
                () -> assertThrows(RuntimeException.class, () -> resolver.resolve("bad\0path")));
    }

    @Test
    void rejectsEscapingAndDanglingLinksButAllowsInternalLinks() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "outside-secret");
        Path internal = Files.createDirectory(gameRoot.resolve("internal"));
        Path internalFile = Files.writeString(internal.resolve("value.txt"), "inside");
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-file.txt"), outsideFile);
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-ancestor"), outside);
        createSymbolicLinkOrSkip(gameRoot.resolve("dangling.txt"), outside.resolve("missing.txt"));
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-link"), internal);
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-file.txt"), internalFile);
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("escaping-file.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("escaping-ancestor/future.txt")),
                () -> assertThrows(IOException.class, () -> resolver.resolve("dangling.txt")),
                () -> assertEquals(gameRoot.resolve("internal-link/value.txt"), resolver.resolve("internal-link/value.txt").path()),
                () -> assertEquals(gameRoot.resolve("internal-file.txt"), resolver.resolve("internal-file.txt").path()));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
