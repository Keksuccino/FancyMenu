package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ResourceLock("PlaceholderParser global state")
class TextHeightPlaceholderTest {

    private static final int LINE_HEIGHT = 9;

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
    void singleLineTextIsOneLineHigh() {
        assertEquals("9", replacement(placeholder(), "Single line"));
        assertEquals("9", replacement(placeholder(), " "));
    }

    @Test
    void emptyTextHasNoHeight() {
        assertEquals("0", replacement(placeholder(), ""));
    }

    @Test
    void missingTextHasNoReplacement() {
        assertNull(replacement(placeholder(), null));
    }

    @Test
    void everySupportedLineBreakStartsANewLine() {
        assertEquals(2, TextHeightPlaceholder.countLines("first%n%second"));
        assertEquals(2, TextHeightPlaceholder.countLines("first\nsecond"));
        assertEquals(2, TextHeightPlaceholder.countLines("first\\nsecond"));
        assertEquals(2, TextHeightPlaceholder.countLines("first\rsecond"));
        assertEquals(5, TextHeightPlaceholder.countLines("a%n%b\nc\\nd\re"));
    }

    @Test
    void windowsLineBreakCountsAsOneLineBreak() {
        assertEquals(2, TextHeightPlaceholder.countLines("first\r\nsecond"));
        assertEquals(3, TextHeightPlaceholder.countLines("first\r\rsecond"));
        assertEquals(3, TextHeightPlaceholder.countLines("first\n\r\nsecond"));
    }

    @Test
    void emptyLinesAtStartMiddleAndEndCount() {
        assertEquals(3, TextHeightPlaceholder.countLines("first%n%%n%second"));
        assertEquals(2, TextHeightPlaceholder.countLines("%n%text"));
        assertEquals(2, TextHeightPlaceholder.countLines("text%n%"));
        assertEquals(2, TextHeightPlaceholder.countLines("\n"));
        assertEquals(3, TextHeightPlaceholder.countLines("\n\n"));
    }

    @Test
    void incompleteLineBreakCodesStayPartOfTheLine() {
        assertEquals(1, TextHeightPlaceholder.countLines("100%n"));
        assertEquals(1, TextHeightPlaceholder.countLines("%n"));
        assertEquals(1, TextHeightPlaceholder.countLines("n%"));
        assertEquals(1, TextHeightPlaceholder.countLines("% n%"));
        assertEquals(1, TextHeightPlaceholder.countLines("ends with a backslash\\"));
        assertEquals(1, TextHeightPlaceholder.countLines("\\t is no line break"));
    }

    @Test
    void overlappingLineBreakCodesGetConsumedFromLeftToRight() {
        assertEquals(2, TextHeightPlaceholder.countLines("%n%n%"));
        assertEquals(3, TextHeightPlaceholder.countLines("%n%%n%"));
        assertEquals(2, TextHeightPlaceholder.countLines("\\\\n"));
    }

    @Test
    void heightIsLineCountTimesTheFontLineHeight() {
        assertEquals("27", replacement(placeholder(), "a%n%b%n%c"));
        assertEquals("21", replacement(new TextHeightPlaceholder(() -> 7), "a%n%b%n%c"));
    }

    @Test
    void lineHeightIsReadOnEveryReplacement() {
        AtomicInteger lineHeight = new AtomicInteger(9);
        TextHeightPlaceholder placeholder = new TextHeightPlaceholder(lineHeight::get);

        assertEquals("18", replacement(placeholder, "a%n%b"));
        lineHeight.set(12);
        assertEquals("24", replacement(placeholder, "a%n%b"));
    }

    @Test
    void lineBreaksInsideParsedPlaceholderValuesArePreserved() {
        registerParserPlaceholder();

        assertEquals("27", PlaceholderParser.replacePlaceholders(serialized("first%n%second%n%third")));
        // Text elements turn %n% into real line breaks before parsing their placeholders
        assertEquals("18", PlaceholderParser.replacePlaceholders(serialized("first\nsecond")));
        assertEquals("0", PlaceholderParser.replacePlaceholders(serialized("")));
    }

    @Test
    void nestedPlaceholderResultsGetMeasured() {
        registerParserPlaceholder();

        assertEquals("18", PlaceholderParser.replacePlaceholders(serialized(serialized("a%n%b") + "%n%c")));
    }

    private static TextHeightPlaceholder placeholder() {
        return new TextHeightPlaceholder(() -> LINE_HEIGHT);
    }

    private static String replacement(TextHeightPlaceholder placeholder, String text) {
        HashMap<String, String> values = new HashMap<>();
        if (text != null) values.put("text", text);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString("text_height", values, ""));
    }

    private static String serialized(String text) {
        return "{\"placeholder\":\"text_height\",\"values\":{\"text\":\"" + text + "\"}}";
    }

    /**
     * The placeholder registry is global and has no removal API, so the parser tests register the placeholder once per test JVM.
     */
    private static void registerParserPlaceholder() {
        if (PlaceholderRegistry.getPlaceholder("text_height") != null) return;
        PlaceholderRegistry.register(new TextHeightPlaceholder(() -> LINE_HEIGHT) {
            // The default check needs a running Minecraft client to compare threads.
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
    }

}
