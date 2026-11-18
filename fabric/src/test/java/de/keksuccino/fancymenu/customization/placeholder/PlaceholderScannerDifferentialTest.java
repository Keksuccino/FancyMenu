package de.keksuccino.fancymenu.customization.placeholder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares {@link PlaceholderScanner} (through {@link PlaceholderParser#findPlaceholders(String, HashMap)}) with a copy of the
 * original scanner, which copied the rest of the text for every brace, on fixed edge cases and many generated texts.
 * The results (start, end and text of every placeholder) must be identical.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderScannerDifferentialTest {

    private static final String[] TOKENS = {
            "{", "{", "{", "}", "}", "}", "\"", "\"", "\"", "\\", "\\", " ", "\t", "\n", "\r", "%n%", "%n%", "%", "n", "%n", ":", ",", "a", "x", "$$",
            "placeholder", "\"placeholder\"", "\"placeholder\":", "\"placeholder\":\"", "{\"placeholder\":\"", "{\"placeholder\":\"", "{ \"placeholder\" : \"",
            "{%n%\"placeholder\"%n%:%n%\"", "{\n\t\"placeholder\"\r\n:\"", "{\"placeholder\":\"x", "{\"placeholder\":\"x\"}", "{\"placeholder\":\"\"}",
            "\"values\":{", "\"text\":\"", "\"}}", "}}", "\",\"", "\\\"", "\\{", "\\}", "\\\\", "{\"a\":\"b\"}", "{ \"placeholder\"x", "{n\"placeholder\":\"",
            "{\"placeholder\" \"x\"}", "{\"place holder\":\"x\"}"
    };
    private static final String OLD_PLACEHOLDER_PREFIX = "{\"placeholder\":\"";

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

    @Test
    void compactPlaceholderIsFound() {
        assertFound("{\"placeholder\":\"a\"}", "{\"placeholder\":\"a\"}");
        assertFound("Hi {\"placeholder\":\"a\"}!", "{\"placeholder\":\"a\"}");
    }

    @Test
    void structuralWhitespaceAroundTheKeyIsAllowed() {
        assertFound("{ \"placeholder\" : \"a\" }", "{ \"placeholder\" : \"a\" }");
        assertFound("{%n%\"placeholder\"%n%:%n%\"a\"}", "{%n%\"placeholder\"%n%:%n%\"a\"}");
        assertFound("{\n\t\"placeholder\"\r\n:\"a\"}", "{\n\t\"placeholder\"\r\n:\"a\"}");
        assertFound("{%n% %n%\"placeholder\":\"a\"}", "{%n% %n%\"placeholder\":\"a\"}");
    }

    @Test
    void anythingElseAroundTheKeyIsNoStart() {
        assertFound("{\"place holder\":\"a\"}");
        assertFound("{\"placeholder\" \"a\"}");
        assertFound("{n\"placeholder\":\"a\"}");
        assertFound("{%n\"placeholder\":\"a\"}");
        assertFound("{%n%n\"placeholder\":\"a\"}");
        assertFound("{\\\"placeholder\":\"a\"}");
        assertFound("{\"placeholder\":a}");
        assertFound("{\"placeholder\":");
        assertFound("\"placeholder\":\"a\"}");
    }

    @Test
    void escapedStartBraceStillStartsAPlaceholder() {
        assertFound("\\{\"placeholder\":\"a\"}", "{\"placeholder\":\"a\"}");
    }

    @Test
    void startWithoutEndIsNoPlaceholder() {
        assertFound("{\"placeholder\":\"a\"");
        assertFound("{\"placeholder\":\"a}");
        assertFound("{\"placeholder\":\"a\"{}");
    }

    @Test
    void placeholderInsideOtherJsonIsFound() {
        assertFound("{\"a\":{\"placeholder\":\"b\"}}", "{\"placeholder\":\"b\"}");
        assertFound("{\"placeholder\" \"a\", {\"placeholder\":\"b\"}}", "{\"placeholder\":\"b\"}");
    }

    @Test
    void nestedPlaceholderIsPartOfItsParent() {
        String nested = "{\"placeholder\":\"a\",\"values\":{\"text\":\"{\"placeholder\":\"b\",\"values\":{\"text\":\"c\"}}\"}}";
        assertFound(nested + " and " + nested, nested, nested);
    }

    @Test
    void bracesInsideQuotesAndEscapedBracesAreIgnored() {
        assertFound("{\"placeholder\":\"a\",\"values\":{\"text\":\"}\"}}", "{\"placeholder\":\"a\",\"values\":{\"text\":\"}\"}}");
        assertFound("{\"placeholder\":\"a\",\"values\":{\"text\":\"\\\"}\"}}", "{\"placeholder\":\"a\",\"values\":{\"text\":\"\\\"}\"}}");
        assertFound("{\"placeholder\":\"a\"\\}}", "{\"placeholder\":\"a\"\\}}");
    }

    @Test
    void placeholdersAfterBrokenStartsAreFound() {
        String broken = "{\"placeholder\":\"x".repeat(50);
        assertFalse(assertSameAsOriginal(broken + "{\"placeholder\":\"y\"}" + broken + "{\"placeholder\":\"z\"}").isEmpty());
        assertFound("{\"placeholder\":\"x {\"placeholder\":\"y\"} {\"placeholder\":\"z\"}", "{\"placeholder\":\"y\"}", "{\"placeholder\":\"z\"}");
        assertFound("{".repeat(500) + "{\"placeholder\":\"y\"}" + "}".repeat(3), "{\"placeholder\":\"y\"}");
    }

    @Test
    void textsWithoutPlaceholdersFindNothing() {
        assertTrue(PlaceholderParser.findPlaceholders(null, new HashMap<>()).isEmpty());
        assertFound("");
        assertFound("{");
        assertFound("{{x}}{\"a\":\"b\"}");
    }

    @Test
    void generatedTextsGiveTheSameResultAsTheOriginalScanner() {
        Random random = new Random(0x5EED_0001L);
        for (int i = 0; i < 100000; i++) {
            assertSameAsOriginal(randomText(random, random.nextInt(48)));
        }
    }

    @Test
    void generatedPlaceholderStructuresGiveTheSameResultAsTheOriginalScanner() {
        Random random = new Random(0x5EED_0002L);
        for (int i = 0; i < 50000; i++) {
            StringBuilder text = new StringBuilder();
            int parts = 1 + random.nextInt(4);
            for (int part = 0; part < parts; part++) {
                if (random.nextInt(3) == 0) text.append(randomText(random, random.nextInt(6)));
                text.append(randomPlaceholder(random, random.nextInt(4)));
            }
            assertSameAsOriginal(corrupt(random, text.toString()));
        }
    }

    @Test
    void longGeneratedTextsGiveTheSameResultAsTheOriginalScanner() {
        Random random = new Random(0x5EED_0003L);
        for (int i = 0; i < 1000; i++) {
            StringBuilder text = new StringBuilder();
            while (text.length() < 3000) {
                int kind = random.nextInt(4);
                if (kind == 0) {
                    text.append(randomText(random, random.nextInt(30)));
                } else if (kind == 1) {
                    text.append("{\"placeholder\":\"broken").append(random.nextInt(10));
                } else {
                    text.append(corrupt(random, randomPlaceholder(random, random.nextInt(3))));
                }
            }
            assertSameAsOriginal(text.toString());
        }
    }

    private static void assertFound(@NotNull String text, @NotNull String... expected) {
        List<Found> found = assertSameAsOriginal(text);
        List<String> placeholders = new ArrayList<>();
        for (Found f : found) placeholders.add(f.placeholder());
        assertEquals(List.of(expected), placeholders, text);
    }

    private static List<Found> assertSameAsOriginal(@NotNull String text) {
        List<Found> expected = originalFindPlaceholders(text);
        List<Found> actual = new ArrayList<>();
        for (PlaceholderParser.ParsedPlaceholder p : PlaceholderParser.findPlaceholders(text, new HashMap<>())) {
            assertEquals(p.placeholderString, text.substring(p.startIndex, p.endIndex));
            actual.add(new Found(p.placeholderString, p.startIndex, p.endIndex));
        }
        assertEquals(expected, actual, () -> "Text: " + escape(text));
        // Every brace in order, so the end table gets used after the first brace without an end
        PlaceholderScanner ends = new PlaceholderScanner(text);
        boolean anyStart = false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '{') continue;
            int index = i;
            assertEquals(originalFindPlaceholderEndIndex(text.substring(i), i), ends.findEnd(i), () -> "End of brace " + index + " in text: " + escape(text));
            boolean originalStart = originalNormalizePlaceholderString(text.substring(i)).startsWith(OLD_PLACEHOLDER_PREFIX);
            assertEquals(originalStart, PlaceholderScanner.matchPlaceholderStart(text, i) >= 0, () -> "Start at brace " + index + " in text: " + escape(text));
            anyStart |= originalStart;
        }
        boolean containsStart = anyStart;
        assertEquals(containsStart, PlaceholderScanner.containsPlaceholderStart(text), () -> "Start check of text: " + escape(text));
        return actual;
    }

    @NotNull
    private static String randomText(@NotNull Random random, int tokens) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens; i++) builder.append(TOKENS[random.nextInt(TOKENS.length)]);
        return builder.toString();
    }

    @NotNull
    private static String randomPlaceholder(@NotNull Random random, int depth) {
        StringBuilder builder = new StringBuilder();
        builder.append('{').append(randomWhitespace(random)).append("\"placeholder\"").append(randomWhitespace(random)).append(':').append(randomWhitespace(random)).append("\"id").append(random.nextInt(5)).append('"');
        int values = random.nextInt(3);
        if (values > 0) {
            builder.append(randomWhitespace(random)).append(',').append(randomWhitespace(random)).append("\"values\"").append(randomWhitespace(random)).append(':').append(randomWhitespace(random)).append('{');
            for (int value = 0; value < values; value++) {
                if (value > 0) builder.append(randomWhitespace(random)).append(',');
                builder.append(randomWhitespace(random)).append('"').append(random.nextBoolean() ? "text" : "other").append('"').append(randomWhitespace(random)).append(':').append(randomWhitespace(random)).append('"');
                if ((depth > 0) && random.nextBoolean()) builder.append(randomPlaceholder(random, depth - 1));
                builder.append(randomText(random, random.nextInt(4)));
                if ((depth > 0) && random.nextInt(3) == 0) builder.append(randomPlaceholder(random, depth - 1));
                builder.append('"');
            }
            builder.append(randomWhitespace(random)).append('}');
        }
        builder.append(randomWhitespace(random)).append('}');
        return builder.toString();
    }

    @NotNull
    private static String randomWhitespace(@NotNull Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> " ";
            case 1 -> "%n%";
            case 2 -> "\n  ";
            case 3 -> "\t";
            default -> "";
        };
    }

    /** Randomly removes, duplicates or inserts a few characters, to get broken starts, unbalanced quotes and braces. */
    @NotNull
    private static String corrupt(@NotNull Random random, @NotNull String text) {
        if (text.isEmpty() || random.nextInt(3) == 0) return text;
        StringBuilder builder = new StringBuilder(text);
        int changes = 1 + random.nextInt(3);
        for (int i = 0; i < changes && builder.length() > 0; i++) {
            int index = random.nextInt(builder.length());
            switch (random.nextInt(3)) {
                case 0 -> builder.deleteCharAt(index);
                case 1 -> builder.insert(index, builder.charAt(index));
                default -> builder.insert(index, "{}\"\\ %".charAt(random.nextInt(6)));
            }
        }
        return builder.toString();
    }

    @NotNull
    private static String escape(@NotNull String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private record Found(@NotNull String placeholder, int start, int end) {
    }

    // ------------------------------------------------------------------------------------------------------------------------
    // Copy of the original scanner of PlaceholderParser, as reference. Do not change it.
    // ------------------------------------------------------------------------------------------------------------------------

    @NotNull
    private static List<Found> originalFindPlaceholders(@NotNull String in) {
        List<Found> placeholders = new ArrayList<>();
        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == '{') {
                String sub = StringUtils.substring(in, i);
                int endIndex = originalFindPlaceholderEndIndex(sub, i);
                if (endIndex != -1) {
                    String candidate = StringUtils.substring(in, i, endIndex + 1);
                    String normalized = originalNormalizePlaceholderString(candidate);
                    if (normalized.startsWith(OLD_PLACEHOLDER_PREFIX)) {
                        placeholders.add(new Found(candidate, i, endIndex + 1));
                        i = endIndex;
                    }
                }
            }
        }
        return placeholders;
    }

    private static int originalFindPlaceholderEndIndex(@NotNull String placeholderStartSubString, int startIndex) {
        int currentIndex = startIndex;
        int depth = 0;
        boolean escaped = false;
        boolean inQuotes = false;
        for (char c : placeholderStartSubString.toCharArray()) {
            if (currentIndex != startIndex) {
                if (!escaped && (c == '"')) {
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    if (!escaped && (c == '{')) {
                        depth++;
                    } else if (!escaped && (c == '}')) {
                        if (depth <= 0) {
                            return currentIndex;
                        }
                        depth--;
                    }
                }
            }
            if (escaped) {
                escaped = false;
            } else {
                escaped = (c == '\\');
            }
            currentIndex++;
        }
        return -1;
    }

    @NotNull
    private static String originalNormalizePlaceholderString(@NotNull String placeholderString) {
        StringBuilder result = new StringBuilder(placeholderString.length());
        boolean inQuotes = false;
        boolean isEscaped = false;
        for (int i = 0; i < placeholderString.length(); i++) {
            char c = placeholderString.charAt(i);
            if (!isEscaped && i + 2 < placeholderString.length() && c == '%' && placeholderString.charAt(i + 1) == 'n' && placeholderString.charAt(i + 2) == '%') {
                if (inQuotes) {
                    result.append("%n%");
                }
                i += 2;
                continue;
            }
            if (isEscaped) {
                result.append('\\');
                result.append(c);
                isEscaped = false;
                continue;
            }
            if (c == '\\') {
                isEscaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                result.append(c);
                continue;
            }
            if (inQuotes) {
                result.append(c);
            } else {
                if (!((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r'))) {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

}
