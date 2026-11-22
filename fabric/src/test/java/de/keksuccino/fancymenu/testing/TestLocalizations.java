package de.keksuccino.fancymenu.testing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads FancyMenu's language files from the test runtime classpath, to check that new texts got localized into every language.
 */
public final class TestLocalizations {

    private static final String ENGLISH_FILE = "/assets/fancymenu/lang/en_us.json";

    private TestLocalizations() {
    }

    /**
     * @return The translations of every language file, by file name, like {@code de_de.json}.
     */
    @NotNull
    public static Map<String, JsonObject> readAllLanguages() throws IOException, URISyntaxException {
        URL english = TestLocalizations.class.getResource(ENGLISH_FILE);
        assertNotNull(english, "English localization resource is missing from the test runtime classpath");
        Path directory = Path.of(english.toURI()).getParent();
        Map<String, JsonObject> languages = new TreeMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    languages.put(file.getFileName().toString(), JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        }
        return languages;
    }

    /**
     * Fails if a key has no translation, or a blank one, in any language file.
     */
    public static void assertLocalizedEverywhere(@NotNull List<String> keys) throws IOException, URISyntaxException {
        Map<String, JsonObject> languages = readAllLanguages();
        assertTrue(languages.size() > 1, "Only found the language files " + languages.keySet());
        for (Map.Entry<String, JsonObject> language : languages.entrySet()) {
            for (String key : keys) {
                JsonElement translation = language.getValue().get(key);
                assertNotNull(translation, "Missing localization in " + language.getKey() + ": " + key);
                assertFalse(translation.getAsString().isBlank(), "Blank localization in " + language.getKey() + ": " + key);
            }
        }
    }

}
