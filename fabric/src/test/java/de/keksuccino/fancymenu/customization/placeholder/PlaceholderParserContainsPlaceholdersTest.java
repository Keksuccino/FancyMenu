package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PlaceholderParser#containsPlaceholders(String)} decides if a text gets parsed at all, so it has to accept every placeholder start
 * that the scanner accepts, also when it is the only placeholder of the text.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserContainsPlaceholdersTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    static Stream<String> spacedPlaceholders() {
        String echo = TestPlaceholders.echoIdentifier();
        return Stream.of(
                "{ \"placeholder\": \"" + echo + "\", \"values\": { \"text\": \"spaced\" } }",
                "{\n  \"placeholder\": \"" + echo + "\",\n  \"values\": {\n    \"text\": \"spaced\"\n  }\n}",
                "{%n%\"placeholder\"%n%:%n%\"" + echo + "\",%n%\"values\":{%n%\"text\":\"spaced\"%n%}%n%}",
                "{\t\"placeholder\"\r\n:\"" + echo + "\",\"values\":{\"text\":\"spaced\"}}"
        );
    }

    @ParameterizedTest
    @MethodSource("spacedPlaceholders")
    void placeholderWithWhitespaceAroundTheKeyGetsParsedOnItsOwn(String placeholder) {
        assertTrue(PlaceholderParser.containsPlaceholders(placeholder));
        assertEquals("spaced", PlaceholderParser.replacePlaceholders(placeholder));
        assertEquals("before spaced after", PlaceholderParser.replacePlaceholders("before " + placeholder + " after"));
    }

    @Test
    void nestedPlaceholderWithWhitespaceAroundTheKeyGetsParsed() {
        String nested = "{ \"placeholder\" : \"" + TestPlaceholders.echoIdentifier() + "\" , \"values\" : { \"text\" : \" deep  value \" } }";
        assertEquals(" deep  value |x", PlaceholderParser.replacePlaceholders(join(nested, "x")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"placeholder\":\"x\"}", "{ \"placeholder\" : \"x\" }", "text {%n% \"placeholder\":\"x\"} text", "{\"placeholder\":\"no end", "\\{\"placeholder\":\"x\"}"})
    void textWithPlaceholderStartContainsPlaceholders(String text) {
        assertTrue(PlaceholderParser.containsPlaceholders(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"placeholder\" \"x\"}", "{\"placeholder\"}", "{\"place holder\":\"x\"}", "{x\"placeholder\":\"y\"}", "{n\"placeholder\":\"y\"}", "\"placeholder\":\"x\"", "{\\\"placeholder\\\":\\\"x\\\"}", "{\"placeholder\":x}", "plain \"placeholder\" text"})
    void textWithoutPlaceholderStartContainsNoPlaceholders(String text) {
        assertFalse(PlaceholderParser.containsPlaceholders(text));
    }

}
