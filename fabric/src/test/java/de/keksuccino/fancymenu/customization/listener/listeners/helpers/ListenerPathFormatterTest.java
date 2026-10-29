package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerPathFormatterTest {

    @TempDir
    Path tempDir;

    @Test
    void pathInsideGameDirectoryBecomesShortInstancePath() {
        Path gameDirectory = this.tempDir.resolve("instance");
        Path screenshot = gameDirectory.resolve("screenshots").resolve("2026-10-29_14.30.00.png");

        assertEquals("/screenshots/2026-10-29_14.30.00.png", this.format(screenshot.toString(), gameDirectory, this.tempDir.resolve("minecraft")));
    }

    @Test
    void nonNormalizedPathInsideGameDirectoryIsNormalized() {
        Path gameDirectory = this.tempDir.resolve("instance");
        String rawPath = gameDirectory + "/screenshots/../screenshots/./image.png";

        assertEquals("/screenshots/image.png", this.format(rawPath, gameDirectory, this.tempDir.resolve("minecraft")));
    }

    @Test
    void gameDirectoryInsideMinecraftDirectoryWinsOverMinecraftPrefix() {
        Path minecraftDirectory = this.tempDir.resolve("minecraft");
        Path gameDirectory = minecraftDirectory.resolve("instances").resolve("modded");
        Path screenshot = gameDirectory.resolve("screenshots").resolve("image.png");

        assertEquals("/screenshots/image.png", this.format(screenshot.toString(), gameDirectory, minecraftDirectory));
    }

    @Test
    void pathInsideMinecraftDirectoryGetsMinecraftPrefix() {
        Path minecraftDirectory = this.tempDir.resolve("minecraft");
        Path file = minecraftDirectory.resolve("screenshots").resolve("image.png");

        assertEquals(".minecraft/screenshots/image.png", this.format(file.toString(), this.tempDir.resolve("instance"), minecraftDirectory));
        assertEquals(".minecraft", this.format(minecraftDirectory.toString(), this.tempDir.resolve("instance"), minecraftDirectory));
    }

    @Test
    void pathOutsideKnownDirectoriesStaysAbsolute() {
        Path file = this.tempDir.resolve("elsewhere").resolve("image.png");
        String expected = file.toAbsolutePath().toString().replace("\\", "/");

        assertEquals(expected, this.format(file.toAbsolutePath().toString(), this.tempDir.resolve("instance"), this.tempDir.resolve("minecraft")));
    }

    @Test
    void minecraftPrefixedPathIsReturnedWithoutResolvingDirectories() {
        Supplier<Path> failingSupplier = () -> { throw new IllegalStateException("Directories must not be resolved for already formatted paths"); };

        assertEquals(".minecraft/screenshots/image.png", ListenerPathFormatter.toFriendlyPath(".minecraft/screenshots/image.png", failingSupplier, failingSupplier));
        assertEquals(".minecraft", ListenerPathFormatter.toFriendlyPath(".minecraft", failingSupplier, failingSupplier));
    }

    @Test
    void failingDirectoryLookupFallsBackToSanitizedPath() {
        Supplier<Path> failingSupplier = () -> { throw new IllegalStateException("Game directory unavailable"); };

        assertEquals("/screenshots/image.png", ListenerPathFormatter.toFriendlyPath("screenshots\\image.png", failingSupplier, failingSupplier));
        assertEquals("C:/instance/screenshots/image.png", ListenerPathFormatter.toFriendlyPath("C:\\instance\\screenshots\\image.png", failingSupplier, failingSupplier));
    }

    @Test
    void missingPathBecomesEmptyString() {
        assertEquals("", this.format(null, this.tempDir.resolve("instance"), this.tempDir.resolve("minecraft")));
        assertEquals("", this.format("   ", this.tempDir.resolve("instance"), this.tempDir.resolve("minecraft")));
    }

    private String format(String rawPath, Path gameDirectory, Path minecraftDirectory) {
        return ListenerPathFormatter.toFriendlyPath(rawPath, () -> gameDirectory, () -> minecraftDirectory);
    }

}
