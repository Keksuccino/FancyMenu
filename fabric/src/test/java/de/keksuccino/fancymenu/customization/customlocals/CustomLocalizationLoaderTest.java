package de.keksuccino.fancymenu.customization.customlocals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CustomLocalizationLoaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void discoversLocaleFilesInRootDirectory() throws IOException {
        write("en_us.json", "{\"root.key\":\"Root value\"}");

        Map<String, String> localizations = load("en_us");

        assertEquals("Root value", localizations.get("root.key"));
    }

    @Test
    void discoversNestedSupportedFormatsAndFlattensJsonObjects() throws IOException {
        write("pack/locale/en_us.json", "{\"nested\":{\"json\":\"JSON value\"}}");
        write("pack/more/en_us.properties", "nested.properties=Properties value\n");
        write("pack/more/en_us.lang", "nested.lang=Lang value\n");

        Map<String, String> localizations = load("en_us");

        assertEquals("JSON value", localizations.get("nested.json"));
        assertEquals("Properties value", localizations.get("nested.properties"));
        assertEquals("Lang value", localizations.get("nested.lang"));
    }

    @Test
    void overlaysSelectedLocaleOnFallbackAndExcludesUnrelatedLocales() throws IOException {
        write("z/en_us.json", "{\"shared\":\"English\",\"fallback.only\":\"Fallback\"}");
        write("a/de_de.json", "{\"shared\":\"Deutsch\",\"selected.only\":\"Ausgewählt\"}");
        write("fr_fr.json", "{\"shared\":\"Français\",\"unrelated\":\"Excluded\"}");

        Map<String, String> localizations = load("de_de");

        assertEquals("Deutsch", localizations.get("shared"));
        assertEquals("Fallback", localizations.get("fallback.only"));
        assertEquals("Ausgewählt", localizations.get("selected.only"));
        assertFalse(localizations.containsKey("unrelated"));
    }

    @Test
    void resolvesDuplicatesBySortedRelativePathInsteadOfCreationOrder() throws IOException {
        write("z/en_us.json", "{\"duplicate\":\"Last path\"}");
        write("a/en_us.json", "{\"duplicate\":\"First path\"}");

        for (int i = 0; i < 5; i++) assertEquals("Last path", load("en_us").get("duplicate"));
    }

    @Test
    void normalizesLocaleCasingWhitespaceAndSeparators() throws IOException {
        write("EN-us.JSON", "{\"normalized\":\"Matched\"}");

        Map<String, String> localizations = load("  en_US  ");

        assertEquals("Matched", localizations.get("normalized"));
    }

    @Test
    void loadsFallbackOnlyOnceWhenItIsSelected() throws IOException {
        write("en_us.json", "{malformed");
        List<Path> failedPaths = new ArrayList<>();

        CustomLocalizationLoader.load(this.temporaryDirectory, "EN-US", (path, exception) -> failedPaths.add(path));

        assertEquals(List.of(this.temporaryDirectory.resolve("en_us.json")), failedPaths);
    }

    @Test
    void usesFallbackWhenSelectedLocaleIsMissing() throws IOException {
        write("en_us.json", "{\"fallback\":\"English\"}");

        Map<String, String> localizations = load("de_de");

        assertEquals(Map.of("fallback", "English"), localizations);
    }

    @Test
    void returnsEmptyMapWhenSelectedAndFallbackLocalesAreMissing() throws IOException {
        write("fr_fr.json", "{\"unrelated\":\"Français\"}");

        Map<String, String> localizations = load("de_de");

        assertTrue(localizations.isEmpty());
    }

    @Test
    void isolatesMalformedFilesAndContinuesLoadingValidFiles() throws IOException {
        write("a/en_us.json", "{malformed");
        write("b/en_us.properties", "fallback=Valid fallback\n");
        write("c/de_de.json", "{\"selected\":\"Valid selected\"}");
        write("d/fr_fr.json", "{also malformed");
        List<Path> failedPaths = new ArrayList<>();

        Map<String, String> localizations = CustomLocalizationLoader.load(this.temporaryDirectory, "de_de", (path, exception) -> failedPaths.add(path));

        assertEquals("Valid fallback", localizations.get("fallback"));
        assertEquals("Valid selected", localizations.get("selected"));
        assertEquals(List.of(this.temporaryDirectory.resolve("a/en_us.json")), failedPaths);
    }

    @Test
    void doesNotLoadFilesThroughSymbolicLinks() throws IOException {
        Path externalFile = write("outside/en_us.json", "{\"external\":\"Excluded\"}");
        Path rootDirectory = this.temporaryDirectory.resolve("root");
        Files.createDirectories(rootDirectory);
        try {
            Files.createSymbolicLink(rootDirectory.resolve("en_us.json"), externalFile);
        } catch (UnsupportedOperationException | FileSystemException exception) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + exception.getMessage());
        }

        Map<String, String> localizations = CustomLocalizationLoader.load(rootDirectory, "en_us", (path, exception) -> {
            throw new AssertionError(exception);
        });

        assertTrue(localizations.isEmpty());
    }

    @Test
    void reloadReplacesThePublishedSnapshotAndRemovesStaleKeys() throws IOException {
        CustomLocalizationStorage storage = new CustomLocalizationStorage();
        Path fallbackFile = write("en_us.json", "{\"old.key\":\"Old value\"}");
        storage.reload(this.temporaryDirectory, "en_us", (path, exception) -> {
            throw new AssertionError(exception);
        });
        Files.delete(fallbackFile);
        write("de_de.json", "{\"new.key\":\"Neuer Wert\"}");

        storage.reload(this.temporaryDirectory, "de_de", (path, exception) -> {
            throw new AssertionError(exception);
        });

        assertEquals("old.key", storage.localize("old.key"));
        assertEquals("Neuer Wert", storage.localize("new.key"));
    }

    private Map<String, String> load(String locale) {
        return CustomLocalizationLoader.load(this.temporaryDirectory, locale, (path, exception) -> {
            throw new AssertionError("Unexpected load failure for " + path, exception);
        });
    }

    private Path write(String relativePath, String content) throws IOException {
        Path file = this.temporaryDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
