package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.util.file.LocalSourcePathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JsonPlaceholderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsRootNestedVirtualAndContainedAbsoluteJsonFiles() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path rootFile = Files.writeString(gameRoot.resolve("root.json"), "{\"value\":\"root-value\"}");
        Path nestedFile = Files.writeString(Files.createDirectories(gameRoot.resolve("config/nested")).resolve("value.json"), "{\"value\":\"nested-value\"}");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        JsonPlaceholder.LocalJsonLookup root = JsonPlaceholder.readLocalJson("root.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup nested = JsonPlaceholder.readLocalJson("/config/nested/value.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup absolute = JsonPlaceholder.readLocalJson(nestedFile.toString(), "$.value", resolver);

        assertAll(
                () -> assertFound(root, "root-value"),
                () -> assertFound(nested, "nested-value"),
                () -> assertFound(absolute, "nested-value"),
                () -> assertEquals(rootFile, resolver.resolve("root.json").path()));
    }

    @Test
    void treatsMinecraftAsALiteralGameDirectoryChild() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Files.writeString(Files.createDirectories(gameRoot.resolve(".minecraft/config")).resolve("value.json"), "{\"value\":\"instance-child\"}");
        Path unrelatedDefaultRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        Files.writeString(Files.createDirectories(unrelatedDefaultRoot.resolve("config")).resolve("value.json"), "{\"value\":\"outside-default\"}");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        JsonPlaceholder.LocalJsonLookup result = JsonPlaceholder.readLocalJson(".minecraft/config/value.json", "$.value", resolver);

        assertFound(result, "instance-child");
    }

    @Test
    void distinguishesMissingFromRejectedSourcesWithoutPublishingOutsideJson() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path sibling = Files.createDirectory(this.temporaryDirectory.resolve("game-backup"));
        Path outsideFile = Files.writeString(sibling.resolve("secret.json"), "{\"value\":\"outside-secret\"}");
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        List<JsonPlaceholder.LocalJsonLookup> rejected = List.of(
                JsonPlaceholder.readLocalJson("../game-backup/secret.json", "$.value", resolver),
                JsonPlaceholder.readLocalJson("nested\\..\\..\\game-backup\\secret.json", "$.value", resolver),
                JsonPlaceholder.readLocalJson(outsideFile.toString(), "$.value", resolver),
                JsonPlaceholder.readLocalJson("C:\\outside\\secret.json", "$.value", resolver),
                JsonPlaceholder.readLocalJson("//server/share/secret.json", "$.value", resolver),
                JsonPlaceholder.readLocalJson("bad\0path", "$.value", resolver));

        assertAll(
                () -> assertEquals(JsonPlaceholder.LocalJsonStatus.MISSING, JsonPlaceholder.readLocalJson("missing.json", "$.value", resolver).status()),
                () -> assertTrue(rejected.stream().allMatch(result -> result.status() == JsonPlaceholder.LocalJsonStatus.REJECTED)),
                () -> assertTrue(rejected.stream().allMatch(result -> result.json() == null)));
    }

    @Test
    void rejectsEscapingAndDanglingLinksWhileReadingInternalLinks() throws Exception {
        Path gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path outside = Files.createDirectory(this.temporaryDirectory.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("secret.json"), "{\"value\":\"outside-secret\"}");
        Path internal = Files.createDirectory(gameRoot.resolve("internal"));
        Path internalFile = Files.writeString(internal.resolve("value.json"), "{\"value\":\"inside-value\"}");
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-file.json"), outsideFile);
        createSymbolicLinkOrSkip(gameRoot.resolve("escaping-directory"), outside);
        createSymbolicLinkOrSkip(gameRoot.resolve("dangling.json"), outside.resolve("missing.json"));
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-directory"), internal);
        createSymbolicLinkOrSkip(gameRoot.resolve("internal-file.json"), internalFile);
        LocalSourcePathResolver resolver = LocalSourcePathResolver.createForGameDirectory(gameRoot);

        JsonPlaceholder.LocalJsonLookup escapingFinal = JsonPlaceholder.readLocalJson("escaping-file.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup escapingAncestor = JsonPlaceholder.readLocalJson("escaping-directory/secret.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup dangling = JsonPlaceholder.readLocalJson("dangling.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup internalDirectory = JsonPlaceholder.readLocalJson("internal-directory/value.json", "$.value", resolver);
        JsonPlaceholder.LocalJsonLookup internalFinal = JsonPlaceholder.readLocalJson("internal-file.json", "$.value", resolver);

        assertAll(
                () -> assertEquals(JsonPlaceholder.LocalJsonStatus.REJECTED, escapingFinal.status()),
                () -> assertEquals(JsonPlaceholder.LocalJsonStatus.REJECTED, escapingAncestor.status()),
                () -> assertEquals(JsonPlaceholder.LocalJsonStatus.REJECTED, dangling.status()),
                () -> assertFound(internalDirectory, "inside-value"),
                () -> assertFound(internalFinal, "inside-value"));
    }

    @Test
    void keepsDirectJsonAndRemoteHttpClassificationSeparateFromLocalFiles() {
        assertAll(
                () -> assertTrue(JsonPlaceholder.isDirectJsonContent("{\"value\":\"direct\"}")),
                () -> assertTrue(JsonPlaceholder.isDirectJsonContent("[1,2,3]")),
                () -> assertTrue(JsonPlaceholder.isHttpSource("https://example.com/data.json")),
                () -> assertTrue(JsonPlaceholder.isHttpSource("http://example.com/data.json")),
                () -> assertTrue(JsonPlaceholder.isHttpSource("HTTPS://example.com/data.json")),
                () -> assertEquals("one%n%two", JsonPlaceholder.formatJsonToString(List.of("one", "two"))));
    }

    private static void assertFound(JsonPlaceholder.LocalJsonLookup lookup, String expectedValue) {
        assertAll(
                () -> assertEquals(JsonPlaceholder.LocalJsonStatus.FOUND, lookup.status()),
                () -> assertEquals(List.of(expectedValue), lookup.json()),
                () -> assertEquals(expectedValue, JsonPlaceholder.formatJsonToString(lookup.json())),
                () -> assertTrue(!lookup.json().contains("outside-secret")));
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            assumeTrue(false, "Symbolic links are unavailable in this test environment: " + ex.getMessage());
        }
    }
}
