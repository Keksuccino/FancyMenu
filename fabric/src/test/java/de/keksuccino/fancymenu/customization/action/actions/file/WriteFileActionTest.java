package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolverFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class WriteFileActionTest {

    private static final String EXPECTED_DEFAULT_CHARSET_PROPERTY = "fancymenu.test.expectedDefaultCharset";

    @TempDir
    Path temporaryDirectory;

    private Path gameRoot;
    private Path minecraftRoot;
    private Path outsideRoot;
    private GameDirectoryActionPathResolver resolver;

    @BeforeAll
    static void verifyConfiguredDefaultCharset() {
        String expectedDefaultCharset = System.getProperty(EXPECTED_DEFAULT_CHARSET_PROPERTY);
        if (expectedDefaultCharset != null) {
            assertEquals(Charset.forName(expectedDefaultCharset), Charset.defaultCharset());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        this.gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        this.minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        this.outsideRoot = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        this.resolver = GameDirectoryActionPathResolverFixture.create(this.gameRoot, this.minecraftRoot);
    }

    @Test
    void overwriteCreatesMissingParentsAsUtf8AndTruncatesExistingContent() throws Exception {
        WriteFileAction action = new WriteFileAction();
        Path target = this.gameRoot.resolve("nested/deep/output.txt");
        String initialContent = "café 雪 😀 𝄞\nnext";

        action.executeWithResolver("nested/deep/output.txt|||café 雪 😀 𝄞\\nnext|||false", this.resolver);

        assertAll(
                () -> assertTrue(Files.isDirectory(target.getParent())),
                () -> assertTrue(Files.isRegularFile(target)),
                () -> assertArrayEquals(initialContent.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target)));

        Files.write(target, "stale trailing bytes".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        action.executeWithResolver("nested/deep/output.txt|||é|||false", this.resolver);
        assertArrayEquals(new byte[]{(byte) 0xC3, (byte) 0xA9}, Files.readAllBytes(target));

        action.executeWithResolver("nested/deep/output.txt||||||false", this.resolver);
        assertArrayEquals(new byte[0], Files.readAllBytes(target));
    }

    @Test
    void appendCreatesMissingFilesAsUtf8AndPreservesExistingContent() throws Exception {
        WriteFileAction action = new WriteFileAction();
        Path target = this.gameRoot.resolve("append/output.txt");
        String firstContent = "é😀";
        String appendedContent = "\n雪𝄞";

        action.executeWithResolver("append/output.txt|||é😀|||true", this.resolver);
        assertArrayEquals(firstContent.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));

        action.executeWithResolver("append/output.txt|||\\n雪𝄞|||true", this.resolver);
        assertArrayEquals((firstContent + appendedContent).getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));

        action.executeWithResolver("append/output.txt||||||true", this.resolver);
        assertArrayEquals((firstContent + appendedContent).getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
    }

    @Test
    void rejectsTraversalAndAllowedRootsWithoutCreatingOrChangingFiles() throws Exception {
        byte[] originalBytes = "outside é😀".getBytes(StandardCharsets.UTF_8);
        Path outsideFile = Files.write(this.outsideRoot.resolve("outside.txt"), originalBytes);
        WriteFileAction action = new WriteFileAction();

        assertAll(
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("../outside/outside.txt|||changed|||false", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver("/|||changed|||false", this.resolver)),
                () -> assertThrows(SecurityException.class, () -> action.executeWithResolver(".minecraft|||changed|||true", this.resolver)),
                () -> assertArrayEquals(originalBytes, Files.readAllBytes(outsideFile)));
    }

    @Test
    void rejectsEscapingFinalSymlinksWithoutChangingTheirTargets() throws Exception {
        byte[] originalBytes = "outside é😀".getBytes(StandardCharsets.UTF_8);
        Path outsideFile = Files.write(this.outsideRoot.resolve("outside.txt"), originalBytes);
        Path escapingLink = this.gameRoot.resolve("escaping.txt");
        createSymbolicLinkOrSkip(escapingLink, outsideFile);

        assertThrows(SecurityException.class, () -> new WriteFileAction().executeWithResolver("escaping.txt|||changed|||false", this.resolver));

        assertAll(
                () -> assertTrue(Files.isSymbolicLink(escapingLink)),
                () -> assertArrayEquals(originalBytes, Files.readAllBytes(outsideFile)));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
