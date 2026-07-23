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

class ConfinedPathResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesTheRootExistingDescendantsAndSafeNonexistentDescendants() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path nested = Files.createDirectories(root.resolve("nested"));
        Path file = Files.writeString(nested.resolve("value.txt"), "inside");
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root);

        assertAll(
                () -> assertEquals(root, resolver.resolve(root).path()),
                () -> assertEquals(file, resolver.resolve(file).revalidate()),
                () -> assertEquals(root.resolve("future/deep/value.txt"), resolver.resolve(root.resolve("future/deep/value.txt")).revalidate()));
    }

    @Test
    void rejectsNormalizedTraversalAndSiblingPrefixPaths() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("../outside.txt"))),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(this.temporaryDirectory.resolve("root-backup/value.txt"))));
    }

    @Test
    void rejectsEscapingFinalAncestorAndDanglingSymbolicLinks() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "outside-secret");
        createSymbolicLinkOrSkip(root.resolve("final.txt"), outsideFile);
        createSymbolicLinkOrSkip(root.resolve("ancestor"), outside);
        createSymbolicLinkOrSkip(root.resolve("dangling.txt"), outside.resolve("missing.txt"));
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root);

        assertAll(
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("final.txt"))),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("ancestor/future.txt"))),
                () -> assertThrows(IOException.class, () -> resolver.resolve(root.resolve("dangling.txt"))));
    }

    @Test
    void permitsInternalSymbolicLinksAndRejectsANewEscapingAncestorAtRevalidation() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path internal = Files.createDirectory(root.resolve("internal"));
        Path internalFile = Files.writeString(internal.resolve("value.txt"), "inside");
        createSymbolicLinkOrSkip(root.resolve("internal-link"), internal);
        createSymbolicLinkOrSkip(root.resolve("internal-file.txt"), internalFile);
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root);
        ConfinedPathResolver.ResolvedPath future = resolver.resolve(root.resolve("future/deep/value.txt"));

        assertAll(
                () -> assertEquals(root.resolve("internal-link/value.txt"), resolver.resolve(root.resolve("internal-link/value.txt")).revalidate()),
                () -> assertEquals(root.resolve("internal-file.txt"), resolver.resolve(root.resolve("internal-file.txt")).revalidate()));

        createSymbolicLinkOrSkip(root.resolve("future"), outside);
        assertThrows(SecurityException.class, future::revalidate);
    }

    @Test
    void rejectsRetargetingACapturedRootSymbolicLink() throws Exception {
        Path firstRoot = Files.createDirectory(this.temporaryDirectory.resolve("first-root"));
        Path secondRoot = Files.createDirectory(this.temporaryDirectory.resolve("second-root"));
        Path rootLink = this.temporaryDirectory.resolve("root-link");
        createSymbolicLinkOrSkip(rootLink, firstRoot);
        ConfinedPathResolver resolver = ConfinedPathResolver.create(rootLink);
        ConfinedPathResolver.ResolvedPath resolved = resolver.resolve(rootLink.resolve("value.txt"));

        Files.delete(rootLink);
        Files.createSymbolicLink(rootLink, secondRoot);

        assertThrows(SecurityException.class, resolved::revalidate);
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
