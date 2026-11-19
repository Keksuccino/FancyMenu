package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.RandomNumberPlaceholder;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.counter;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.perOccurrenceCounter;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Placeholders that return a new value on every call get replaced at every occurrence instead of sharing one replacement,
 * see {@link Placeholder#isEvaluatedPerOccurrence(DeserializedPlaceholderString)}.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserPerOccurrenceTest {

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
    void everyOccurrenceGetsItsOwnReplacement() {
        String placeholder = perOccurrenceCounter("per_occurrence_top");
        assertEquals(List.of("1", "2", "3"), sortedWords(PlaceholderParser.replacePlaceholders(placeholder + " " + placeholder + " " + placeholder)));
    }

    @Test
    void identicalPlaceholdersThatAreNotEvaluatedPerOccurrenceStillShareTheirReplacement() {
        String placeholder = counter("per_occurrence_shared");
        assertEquals("1 1|1", PlaceholderParser.replacePlaceholders(placeholder + " " + join(placeholder, echo(placeholder))));
        assertEquals("2 2", PlaceholderParser.replacePlaceholders(placeholder + " " + placeholder));
    }

    @Test
    void placeholdersWithSuchAPlaceholderInTheirValuesGetReplacedAtEveryOccurrence() {
        String nested = echo(perOccurrenceCounter("per_occurrence_nested"));
        assertEquals(List.of("1", "2"), sortedWords(PlaceholderParser.replacePlaceholders(nested + " " + nested)));
        String deep = join(echo(echo(perOccurrenceCounter("per_occurrence_deep"))), "x");
        assertEquals(List.of("1|x", "2|x"), sortedWords(PlaceholderParser.replacePlaceholders(deep + " " + deep)));
    }

    @Test
    void placeholdersWithOtherValuesStillShareTheirReplacement() {
        String nested = echo(perOccurrenceCounter("per_occurrence_mixed"));
        String plain = value("per_occurrence_mixed_plain");
        TestPlaceholders.setValue("per_occurrence_mixed_plain", "p");
        assertEquals(List.of("1", "2", "p", "p"), sortedWords(PlaceholderParser.replacePlaceholders(nested + " " + plain + " " + nested + " " + plain)));
        assertEquals(1, TestPlaceholders.evaluations("per_occurrence_mixed_plain"));
    }

    @Test
    void occurrencesThatComeFromOutputGetTheirOwnReplacement() {
        TestPlaceholders.setValue("per_occurrence_output", perOccurrenceCounter("per_occurrence_output_counter"));
        String placeholder = value("per_occurrence_output");
        assertEquals(List.of("1", "2"), sortedWords(PlaceholderParser.replacePlaceholders(placeholder + " " + placeholder)));
    }

    @Test
    void parsedTextsStillGetCached() {
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> true, () -> 60000L));
        String placeholder = perOccurrenceCounter("per_occurrence_cached");
        String text = "cached " + placeholder + " " + placeholder;
        String first = PlaceholderParser.replacePlaceholders(text);
        assertEquals(first, PlaceholderParser.replacePlaceholders(text));
        assertEquals(List.of("1", "2", "cached"), sortedWords(first));
    }

    @Test
    void parsedPlaceholderUsesTheFlagForItsValues() {
        String nested = echo(perOccurrenceCounter("per_occurrence_parsed"));
        HashMap<String, String> parsed = new HashMap<>();
        PlaceholderParser.ParsedPlaceholder first = PlaceholderParser.findPlaceholders(join(nested, nested), parsed).get(0);
        String replacement = first.getReplacement();
        assertTrue(replacement.equals("1|2") || replacement.equals("2|1"), replacement);
    }

    @Test
    void placeholdersAreNotEvaluatedPerOccurrenceByDefault() {
        Placeholder echo = PlaceholderRegistry.getPlaceholder(TestPlaceholders.echoIdentifier());
        assertFalse(echo.isEvaluatedPerOccurrence(dps(echo.getIdentifier())));
    }

    @Test
    void randomNumberIsEvaluatedPerOccurrence() {
        RandomNumberPlaceholder randomNumber = new RandomNumberPlaceholder();
        DeserializedPlaceholderString dps = dps(randomNumber.getIdentifier());
        dps.values.put("min", "1");
        dps.values.put("max", "100");
        assertTrue(randomNumber.isEvaluatedPerOccurrence(dps));
    }

    /** The words of the text, sorted, because the order in which occurrences get replaced is not part of the contract. */
    @NotNull
    private static List<String> sortedWords(@NotNull String text) {
        String[] words = text.split(" ");
        Arrays.sort(words);
        return List.of(words);
    }

    @NotNull
    private static DeserializedPlaceholderString dps(@NotNull String identifier) {
        return new DeserializedPlaceholderString(identifier, null, "{\"placeholder\":\"" + identifier + "\"}");
    }

}
