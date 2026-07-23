package de.keksuccino.fancymenu.util.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GameDirectoryActionPathResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void mapsLegacyAndAbsoluteGamePathsToTheGameRoot() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        GameDirectoryActionPathResolver.ResolvedPath relative = resolver.resolve("config/fancymenu/settings.txt");
        GameDirectoryActionPathResolver.ResolvedPath legacyRooted = resolver.resolve("/config/fancymenu/settings.txt");
        GameDirectoryActionPathResolver.ResolvedPath absolute = resolver.resolve(gameRoot.resolve("config/settings.txt").toString());

        assertAll(
                () -> assertEquals(gameRoot.resolve("config/fancymenu/settings.txt"), relative.path()),
                () -> assertEquals(relative.path(), legacyRooted.path()),
                () -> assertEquals(gameRoot.resolve("config/settings.txt"), absolute.path()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.GAME_DIRECTORY, relative.allowedRoot()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.GAME_DIRECTORY, absolute.allowedRoot()));
    }

    @Test
    void mapsOnlyExplicitMinecraftPathsToTheDefaultMinecraftRoot() throws Exception {
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path gameRoot = Files.createDirectories(minecraftRoot.resolve("instances/game"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        GameDirectoryActionPathResolver.ResolvedPath shorthand = resolver.resolve(".minecraft/config/options.txt");
        GameDirectoryActionPathResolver.ResolvedPath minecraftAbsolute = resolver.resolve(minecraftRoot.resolve("resourcepacks/pack.zip").toString());
        GameDirectoryActionPathResolver.ResolvedPath overlappingAbsolute = resolver.resolve(gameRoot.resolve("config/settings.txt").toString());
        GameDirectoryActionPathResolver.ResolvedPath explicitOverlappingShorthand = resolver.resolve(".minecraft/instances/game/config/other.txt");

        assertAll(
                () -> assertEquals(minecraftRoot.resolve("config/options.txt"), shorthand.path()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, shorthand.allowedRoot()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraftAbsolute.allowedRoot()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.GAME_DIRECTORY, overlappingAbsolute.allowedRoot()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, explicitOverlappingShorthand.allowedRoot()));
    }

    @Test
    void rejectsTraversalForBothAdvertisedRootsAndSeparatorStyles() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("../../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("..\\..\\outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("/safe/../../../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft/../../outside.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft\\..\\outside.txt")));
    }

    @Test
    void rejectsSiblingPrefixTricks() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        Path gameSibling = this.temporaryDirectory.resolve("game-backup/file.txt").toAbsolutePath();
        Path minecraftSibling = this.temporaryDirectory.resolve("minecraft-old/file.txt").toAbsolutePath();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(gameSibling.toString())),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(minecraftSibling.toString())));
    }

    @Test
    void neverAuthorizesAnOutsideAbsolutePath() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outsidePath = this.temporaryDirectory.resolve("elsewhere/file.txt").toAbsolutePath();
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        try {
            GameDirectoryActionPathResolver.ResolvedPath resolved = resolver.resolve(outsidePath.toString());
            assertTrue(resolved.path().startsWith(gameRoot));
            assertFalse(resolved.path().startsWith(this.temporaryDirectory.resolve("elsewhere")));
        } catch (SecurityException expectedOnPlatformsWithDriveAbsolutePaths) {
            assertFalse(outsidePath.startsWith(gameRoot));
            assertFalse(outsidePath.startsWith(minecraftRoot));
        }
    }

    @Test
    void rejectsForeignDriveAndUncForms() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("C:\\outside\\file.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("C:outside\\file.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("//server/share/file.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("\\\\server\\share\\file.txt")));
    }

    @Test
    void allowsNonExistingDescendantsThroughExistingSafeAncestors() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Files.createDirectory(gameRoot.resolve("config"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        GameDirectoryActionPathResolver.ResolvedPath resolved = resolver.resolve("config/new/deep/file.txt");

        assertEquals(gameRoot.resolve("config/new/deep/file.txt"), resolved.path());
        assertEquals(resolved.path(), resolved.revalidate());
    }

    @Test
    void projectsAndRevalidatesRootsThatDoNotExistYet() throws Exception {
        Path gameRoot = this.temporaryDirectory.resolve("missing-game");
        Path minecraftRoot = this.temporaryDirectory.resolve("missing-minecraft");
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);
        GameDirectoryActionPathResolver.ResolvedPath resolved = resolver.resolve("config/new/file.txt");

        Files.createDirectories(resolved.path().getParent());
        Files.createFile(resolved.path());

        assertEquals(gameRoot.resolve("config/new/file.txt"), resolved.revalidate());
    }

    @Test
    void rejectsANewEscapingSymlinkThatReplacesAMissingAncestor() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outsideDirectory = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);
        GameDirectoryActionPathResolver.ResolvedPath resolved = resolver.resolve("future/deep/file.txt");

        createSymbolicLinkOrSkip(gameRoot.resolve("future"), outsideDirectory);

        assertThrows(SecurityException.class, resolved::revalidate);
    }

    @Test
    void rejectsEscapingAndDanglingSymlinkAncestorsAndFinalTargets() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path outsideDirectory = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outsideDirectory.resolve("outside.txt"), "outside");
        Path escapingDirectoryLink = gameRoot.resolve("escaping-directory");
        Path escapingFileLink = gameRoot.resolve("escaping-file.txt");
        Path danglingLink = gameRoot.resolve("dangling-file.txt");
        createSymbolicLinkOrSkip(escapingDirectoryLink, outsideDirectory);
        createSymbolicLinkOrSkip(escapingFileLink, outsideFile);
        createSymbolicLinkOrSkip(danglingLink, outsideDirectory.resolve("missing.txt"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("escaping-directory/new.txt")),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("escaping-file.txt")),
                () -> assertThrows(IOException.class, () -> resolver.resolve("dangling-file.txt")));
    }

    @Test
    void permitsSymlinksWhoseRealTargetsRemainInsideTheSelectedRoot() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path actualDirectory = Files.createDirectory(gameRoot.resolve("actual"));
        Path actualFile = Files.writeString(actualDirectory.resolve("file.txt"), "inside");
        Path directoryLink = gameRoot.resolve("directory-link");
        Path fileLink = gameRoot.resolve("file-link.txt");
        createSymbolicLinkOrSkip(directoryLink, actualDirectory);
        createSymbolicLinkOrSkip(fileLink, actualFile);
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        assertAll(
                () -> assertEquals(directoryLink.resolve("new.txt"), resolver.resolve("directory-link/new.txt").path()),
                () -> assertEquals(fileLink, resolver.resolve("file-link.txt").path()));
    }

    @Test
    void permitsARootSymlinkButRejectsRetargetingItLater() throws Exception {
        Path realGameRoot = Files.createDirectory(this.temporaryDirectory.resolve("real-game"));
        Path replacementRoot = Files.createDirectory(this.temporaryDirectory.resolve("replacement-game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Path gameRootLink = this.temporaryDirectory.resolve("game-link");
        createSymbolicLinkOrSkip(gameRootLink, realGameRoot);
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRootLink, minecraftRoot);
        GameDirectoryActionPathResolver.ResolvedPath resolved = resolver.resolve("config/file.txt");

        assertEquals(gameRootLink.resolve("config/file.txt"), resolved.path());
        Files.delete(gameRootLink);
        Files.createSymbolicLink(gameRootLink, replacementRoot);

        assertThrows(SecurityException.class, resolved::revalidate);
    }

    @Test
    void childApisKeepTheirParentScopeAndSelectedRoot() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);
        GameDirectoryActionPathResolver.ResolvedPath parent = resolver.resolve(".minecraft/extracted");

        GameDirectoryActionPathResolver.ResolvedPath nested = parent.resolveRelativeChild("assets/textures/file.png");
        GameDirectoryActionPathResolver.ResolvedPath sibling = nested.resolveSingleComponentSibling("renamed.png");

        assertAll(
                () -> assertEquals(minecraftRoot.resolve("extracted/assets/textures/file.png"), nested.path()),
                () -> assertEquals(minecraftRoot.resolve("extracted/assets/textures/renamed.png"), sibling.path()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, nested.allowedRoot()),
                () -> assertEquals(GameDirectoryActionPathResolver.AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, sibling.allowedRoot()),
                () -> assertThrows(SecurityException.class, () -> parent.resolveRelativeChild("../outside")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveRelativeChild("directory/../")));
    }

    @Test
    void singleComponentChildrenRejectSeparatorsDotNamesAndQualifiedPaths() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);
        GameDirectoryActionPathResolver.ResolvedPath parent = resolver.resolve("config");
        GameDirectoryActionPathResolver.ResolvedPath source = parent.resolveSingleComponentChild("source.txt");

        assertAll(
                () -> assertEquals(gameRoot.resolve("config/renamed.txt"), parent.resolveSingleComponentChild("renamed.txt").path()),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild("")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild(".")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild("..")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild("nested/name.txt")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild("nested\\name.txt")),
                () -> assertThrows(SecurityException.class, () -> parent.resolveSingleComponentChild("C:\\name.txt")),
                () -> assertThrows(SecurityException.class, () -> source.resolveSingleComponentSibling(".")),
                () -> assertThrows(SecurityException.class, () -> source.resolveSingleComponentSibling("..")),
                () -> assertThrows(SecurityException.class, () -> source.resolveSingleComponentSibling("nested/name.txt")),
                () -> assertThrows(SecurityException.class, () -> source.resolveSingleComponentSibling("nested\\name.txt")));
    }

    @Test
    void rootSelectionsMustBeRejectedByDestructiveOperations() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        GameDirectoryActionPathResolver resolver = GameDirectoryActionPathResolver.create(gameRoot, minecraftRoot);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("").requireDescendant()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("/").requireDescendant()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".").requireDescendant()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve("directory/..").requireDescendant()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft").requireDescendant()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(".minecraft/directory/..").requireDescendant()));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
