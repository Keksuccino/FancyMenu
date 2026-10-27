package de.keksuccino.fancymenu.util.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void relocatedDirectoriesExtendTheBoundaryOnlyToTheirCapturedTargets() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path relocated = Files.createDirectories(this.temporaryDirectory.resolve("shared/relocated"));
        Path relocatedFile = Files.writeString(Files.createDirectory(relocated.resolve("assets")).resolve("value.txt"), "relocated");
        Files.createDirectory(root.resolve("config"));
        createSymbolicLinkOrSkip(root.resolve("config/relocated"), relocated);
        createSymbolicLinkOrSkip(relocated.resolve("escaping"), outside);
        createSymbolicLinkOrSkip(root.resolve("escaping"), outside);
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root, List.of("config/relocated"));
        ConfinedPathResolver withoutRelocation = ConfinedPathResolver.create(root);

        assertAll(
                () -> assertEquals(root.resolve("config/relocated/assets/value.txt"), resolver.resolve(root.resolve("config/relocated/assets/value.txt")).revalidate()),
                () -> assertEquals(root.resolve("config/relocated/future/value.txt"), resolver.resolve(root.resolve("config/relocated/future/value.txt")).revalidate()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("config/relocated/escaping/secret.txt"))),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("escaping/secret.txt"))),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(relocatedFile)),
                () -> assertThrows(SecurityException.class, () -> withoutRelocation.resolve(root.resolve("config/relocated/assets/value.txt"))));
    }

    @Test
    void relocatedAncestorsOnlyExposeTheRelocatableDirectory() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path sharedConfig = Files.createDirectories(this.temporaryDirectory.resolve("shared-config/relocated")).getParent();
        Files.writeString(sharedConfig.resolve("other.txt"), "other");
        createSymbolicLinkOrSkip(root.resolve("config"), sharedConfig);
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root, List.of("config/relocated"));

        assertAll(
                () -> assertEquals(root.resolve("config/relocated/value.txt"), resolver.resolve(root.resolve("config/relocated/value.txt")).revalidate()),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("config/other.txt"))));
    }

    @Test
    void doesNotFollowRelocatableDirectoriesRetargetedOrLinkedAfterCapture() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path firstTarget = Files.createDirectory(this.temporaryDirectory.resolve("first-target"));
        Path secondTarget = Files.createDirectory(this.temporaryDirectory.resolve("second-target"));
        Path relocatedLink = root.resolve("relocated");
        createSymbolicLinkOrSkip(relocatedLink, firstTarget);
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root, List.of("relocated", "late"));
        ConfinedPathResolver.ResolvedPath resolved = resolver.resolve(relocatedLink.resolve("value.txt"));

        Files.delete(relocatedLink);
        Files.createSymbolicLink(relocatedLink, secondTarget);
        Files.createSymbolicLink(root.resolve("late"), secondTarget);

        assertAll(
                () -> assertThrows(SecurityException.class, resolved::revalidate),
                () -> assertThrows(SecurityException.class, () -> resolver.resolve(root.resolve("late/value.txt"))));
    }

    @Test
    void rejectsRelocatableDirectoriesOutsideTheRootAndIgnoresDanglingOnes() throws Exception {
        Path root = Files.createDirectory(this.temporaryDirectory.resolve("root"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        createSymbolicLinkOrSkip(root.resolve("dangling"), outside.resolve("missing"));
        ConfinedPathResolver resolver = ConfinedPathResolver.create(root, List.of("dangling"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> ConfinedPathResolver.create(root, List.of(""))),
                () -> assertThrows(IllegalArgumentException.class, () -> ConfinedPathResolver.create(root, List.of("../outside"))),
                () -> assertThrows(IllegalArgumentException.class, () -> ConfinedPathResolver.create(root, List.of(outside.toString()))),
                () -> assertEquals(root.resolve("value.txt"), resolver.resolve(root.resolve("value.txt")).revalidate()),
                () -> assertThrows(IOException.class, () -> resolver.resolve(root.resolve("dangling/value.txt"))));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
