package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestLanguage;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser.MAX_TEXT_LENGTH;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Texts and values of nested placeholders of up to {@link PlaceholderParser#MAX_TEXT_LENGTH} characters get parsed, longer ones get answered
 * with the translated error text, unless they get parsed without length limit. Texts of the maximum length still fit into every cache they go through.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserTextLengthLimitTest {

    /** How many texts of the maximum length every cache has to hold at once, so layouts with several long texts don't recompute them every frame. */
    private static final int LONG_TEXTS_PER_CACHE = 8;

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
    void textOfExactlyTheMaximumLengthGetsParsed() {
        TestPlaceholders.setValue("length_limit_exact", "replaced");
        String placeholder = value("length_limit_exact");
        String text = fill(placeholder, MAX_TEXT_LENGTH);
        String expected = "replaced" + text.substring(placeholder.length());

        assertEquals(expected, PlaceholderParser.replacePlaceholders(text));
        assertEquals(expected, PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(text));
    }

    @Test
    void textLongerThanTheMaximumLengthGetsTheErrorText() {
        String text = fill(value("length_limit_over"), MAX_TEXT_LENGTH + 1);

        assertEquals(PlaceholderParser.getTextTooLongMessage(), PlaceholderParser.replacePlaceholders(text));
        assertEquals(PlaceholderParser.getTextTooLongMessage(), PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(text));
        assertEquals(0, TestPlaceholders.evaluations("length_limit_over"));
    }

    @Test
    void nestedValueOfExactlyTheMaximumLengthGetsParsed() {
        TestPlaceholders.setValue("length_limit_nested_exact_inner", "inner");
        String innerPlaceholder = value("length_limit_nested_exact_inner");
        String nestedValue = fill(innerPlaceholder, MAX_TEXT_LENGTH);
        String expected = "inner" + nestedValue.substring(innerPlaceholder.length());
        // The value can only be this long in placeholder output, which gets parsed again, or in placeholders found in a longer text
        TestPlaceholders.setValue("length_limit_nested_exact_outer", echo(nestedValue));

        assertEquals(expected, PlaceholderParser.replacePlaceholders(value("length_limit_nested_exact_outer")));
        assertEquals(expected, findSinglePlaceholder(echo(nestedValue)).getReplacement());
    }

    @Test
    void nestedValueLongerThanTheMaximumLengthGetsTheErrorText() {
        String nestedValue = fill(value("length_limit_nested_over_inner"), MAX_TEXT_LENGTH + 1);
        TestPlaceholders.setValue("length_limit_nested_over_outer", echo(nestedValue));

        assertEquals(PlaceholderParser.getTextTooLongMessage(), PlaceholderParser.replacePlaceholders(value("length_limit_nested_over_outer")));
        assertEquals(PlaceholderParser.getTextTooLongMessage(), findSinglePlaceholder(echo(nestedValue)).getReplacement());
        assertEquals(0, TestPlaceholders.evaluations("length_limit_nested_over_inner"));
    }

    @Test
    void parsingWithoutLengthLimitParsesLongerTextsAndValues() {
        TestPlaceholders.setValue("length_limit_unlimited", "replaced");
        String placeholder = value("length_limit_unlimited");
        String text = fill(placeholder, MAX_TEXT_LENGTH + 1);
        String expected = "replaced" + text.substring(placeholder.length());

        assertEquals(expected, PlaceholderParser.replacePlaceholdersWithoutLengthLimit(text, false));
        assertEquals(expected, PlaceholderParser.replacePlaceholdersWithoutLengthLimit(text, true));
        assertEquals(expected, PlaceholderParser.replacePlaceholdersWithoutLengthLimit(echo(text), false));
    }

    @Test
    void errorTextIsTheTranslationOfItsKeyWithTheMaximumLengthAsFormattedArgument() {
        String tooLong = fill("", MAX_TEXT_LENGTH + 1);
        TestLanguage language = new TestLanguage(Map.of(PlaceholderParser.TEXT_TOO_LONG_LOCALIZATION_KEY, "[%s]", LocalizationUtils.DIGIT_GROUP_SEPARATOR_KEY, "'"));
        language.runAsGameLanguage(() -> {
            String expected = "[" + LocalizationUtils.formatWholeNumber(MAX_TEXT_LENGTH, language) + "]";
            assertTrue(expected.contains("'"));
            assertEquals(expected, PlaceholderParser.replacePlaceholders(tooLong));
            // The text only gets translated again when the language changes
            assertSame(PlaceholderParser.getTextTooLongMessage(), PlaceholderParser.getTextTooLongMessage());
        });
        TestLanguage otherLanguage = new TestLanguage(Map.of(PlaceholderParser.TEXT_TOO_LONG_LOCALIZATION_KEY, "<%s>"));
        otherLanguage.runAsGameLanguage(() -> assertEquals("<" + LocalizationUtils.formatWholeNumber(MAX_TEXT_LENGTH, otherLanguage) + ">", PlaceholderParser.replacePlaceholders(tooLong)));
    }

    @Test
    void errorTranslationWithBrokenFormatGetsUsedAsItIs() {
        TestLanguage language = new TestLanguage(Map.of(PlaceholderParser.TEXT_TOO_LONG_LOCALIZATION_KEY, "broken %d"));
        language.runAsGameLanguage(() -> assertEquals("broken %d", PlaceholderParser.replacePlaceholders(fill("", MAX_TEXT_LENGTH + 1))));
    }

    @Test
    void textsOfTheMaximumLengthGetCachedByEveryCacheTheyGoThrough() {
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 600_000L));
        PlaceholderEvaluator.ParsingContext context = PlaceholderEvaluator.createParsingContext(false, true);
        String[] texts = new String[LONG_TEXTS_PER_CACHE];
        String[] compiled = new String[LONG_TEXTS_PER_CACHE];
        PlaceholderTemplate[] templates = new PlaceholderTemplate[LONG_TEXTS_PER_CACHE];
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            TestPlaceholders.setValue(outerKey(i), "outer");
            TestPlaceholders.setValue(innerKey(i), "inner");
            // The line break code makes the compiled text a new string, which makes the cache of compiled texts hold the text twice.
            // The nested value gets its result cached in the value cache. Every text is one character shorter than the maximum, for the variants below.
            String start = TextEditorWindowBody.NEWLINE_CODE + value(outerKey(i)) + " ";
            String nestedValue = fill(value(innerKey(i)) + " " + i + " ", MAX_TEXT_LENGTH - 1 - start.length() - echo("").length());
            texts[i] = start + echo(nestedValue);
            assertEquals(MAX_TEXT_LENGTH - 1, texts[i].length());
        }

        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            compiled[i] = TextEditorWindowBody.compileSingleLineString(texts[i]);
            assertTrue(PlaceholderParser.replacePlaceholders(texts[i]).startsWith("outer inner " + i + " "));
            templates[i] = PlaceholderTemplate.get(compiled[i], context);
        }
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            assertTrue(PlaceholderParser.replacePlaceholders(texts[i]).startsWith("outer inner " + i + " "));
            assertSame(compiled[i], TextEditorWindowBody.compileSingleLineString(texts[i]), "compiled text " + i);
            assertSame(templates[i], PlaceholderTemplate.get(compiled[i], context), "template " + i);
            assertNotNull(PlaceholderEvaluator.CONTAINS_PLACEHOLDERS.get(compiled[i]), "placeholder check " + i);
        }
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            // Every result of the second round came from the text result cache
            assertEquals(1, TestPlaceholders.evaluations(outerKey(i)), "text result " + i);
        }

        // Texts that differ from the cached ones outside of their nested value get their nested value from the value cache
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            String variant = texts[i] + " ";
            assertEquals(MAX_TEXT_LENGTH, variant.length());
            assertTrue(PlaceholderParser.replacePlaceholders(variant).startsWith("outer inner " + i + " "));
        }
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            assertEquals(2, TestPlaceholders.evaluations(outerKey(i)), "variant text " + i);
            assertEquals(1, TestPlaceholders.evaluations(innerKey(i)), "value result " + i);
        }
    }

    @Test
    void logCooldownHoldsErrorsWithPlaceholdersOfTheMaximumLength() {
        // Errors of placeholders whose values can't be read contain their raw text, see PlaceholderTemplate
        long now = System.currentTimeMillis();
        String[] errors = new String[LONG_TEXTS_PER_CACHE];
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            errors[i] = "[FANCYMENU] Failed to parse values of placeholder: " + fill("length_limit_log_" + i + " ", MAX_TEXT_LENGTH);
            assertTrue(PlaceholderEvaluator.LOG_COOLDOWN.tryAcquire(errors[i], now), "first error " + i);
        }
        for (int i = 0; i < LONG_TEXTS_PER_CACHE; i++) {
            assertFalse(PlaceholderEvaluator.LOG_COOLDOWN.tryAcquire(errors[i], now), "repeated error " + i);
        }
    }

    @NotNull
    private static PlaceholderParser.ParsedPlaceholder findSinglePlaceholder(@NotNull String text) {
        List<PlaceholderParser.ParsedPlaceholder> placeholders = PlaceholderParser.findPlaceholders(text, new HashMap<>());
        assertEquals(1, placeholders.size());
        return placeholders.get(0);
    }

    @NotNull
    private static String outerKey(int index) {
        return "length_limit_cache_outer_" + index;
    }

    @NotNull
    private static String innerKey(int index) {
        return "length_limit_cache_inner_" + index;
    }

    /**
     * Returns the start, filled up with short words to exactly the given length.
     */
    @NotNull
    private static String fill(@NotNull String start, int length) {
        StringBuilder builder = new StringBuilder(length + 5).append(start);
        while (builder.length() < length) builder.append("word ");
        builder.setLength(length);
        return builder.toString();
    }

}
