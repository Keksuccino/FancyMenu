package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.capture;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.nullReplacement;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * How the parser replaces the placeholders of a text: by position, once per identical placeholder, again in their output,
 * and with raw text for every placeholder that can't get replaced.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserEvaluationTest {

    private static final String UNKNOWN = "{\"placeholder\":\"evaluation_test_unknown\",\"values\":{\"text\":\"x\"}}";

    private final List<Long> processorIds = new ArrayList<>();
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreGlobalState() {
        for (long id : this.processorIds) PlaceholderParser.removeParsingProcessor(id);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void identicalPlaceholdersGetReplacedOnceAcrossNestingLevels() {
        TestPlaceholders.setValue("evaluation_memo", "m");
        String placeholder = value("evaluation_memo");
        assertEquals("m m m|m", PlaceholderParser.replacePlaceholders(placeholder + " " + placeholder + " " + join(placeholder, echo(placeholder))));
        assertEquals(1, TestPlaceholders.evaluations("evaluation_memo"));
    }

    @Test
    void identicalPlaceholdersShareTheirReplacementAcrossPasses() {
        TestPlaceholders.setValue("evaluation_memo_outer", value("evaluation_memo_inner"));
        TestPlaceholders.setValue("evaluation_memo_inner", "i");
        assertEquals("i i", PlaceholderParser.replacePlaceholders(value("evaluation_memo_outer") + " " + value("evaluation_memo_inner")));
        assertEquals(1, TestPlaceholders.evaluations("evaluation_memo_inner"));
    }

    @Test
    void everyParseReplacesItsPlaceholdersAgain() {
        TestPlaceholders.setValue("evaluation_parse", "first");
        String text = "a " + value("evaluation_parse");
        assertEquals("a first", PlaceholderParser.replacePlaceholders(text));
        TestPlaceholders.setValue("evaluation_parse", "second");
        assertEquals("a second", PlaceholderParser.replacePlaceholders(text));
        assertEquals(2, TestPlaceholders.evaluations("evaluation_parse"));
    }

    @Test
    void placeholdersInOutputGetReplacedOverAsManyPassesAsNeeded() {
        TestPlaceholders.setValue("evaluation_pass_1", "a " + value("evaluation_pass_2"));
        TestPlaceholders.setValue("evaluation_pass_2", "b " + value("evaluation_pass_3"));
        TestPlaceholders.setValue("evaluation_pass_3", "c " + echo("d"));
        assertEquals("[a b c d]", PlaceholderParser.replacePlaceholders("[" + value("evaluation_pass_1") + "]"));
        assertEquals("a b c d|x", PlaceholderParser.replacePlaceholders(join(value("evaluation_pass_1"), "x")));
    }

    @Test
    void placeholderThatStartsInOutputAndEndsInTheTextGetsReplaced() {
        TestPlaceholders.setValue("evaluation_split_start", "{\"placeholder\":\"");
        String text = value("evaluation_split_start") + TestPlaceholders.echoIdentifier() + "\",\"values\":{\"text\":\"joined\"}}";
        assertEquals("joined", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void placeholderThatReturnsItselfStopsAfterOnePass() {
        String text = value("evaluation_self");
        TestPlaceholders.setValue("evaluation_self", text);
        assertEquals(text, PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void placeholdersThatCantGetReplacedKeepTheirRawText() {
        String noIdentifier = "{\"placeholder\":\"\"}";
        String noEnd = "{\"placeholder\":\"" + TestPlaceholders.echoIdentifier() + "\",\"values\":{\"text\":\"x\"}";
        assertEquals(UNKNOWN + " a " + noIdentifier + " b", PlaceholderParser.replacePlaceholders(UNKNOWN + " " + echo("a") + " " + noIdentifier + " b"));
        assertEquals(UNKNOWN + "|" + noIdentifier, PlaceholderParser.replacePlaceholders(join(UNKNOWN, noIdentifier)));
        assertEquals("x " + noEnd, PlaceholderParser.replacePlaceholders(echo("x") + " " + noEnd));
    }

    @Test
    void nullReplacementKeepsTheRawText() {
        String placeholder = nullReplacement("x");
        assertEquals("a " + placeholder + " b", PlaceholderParser.replacePlaceholders("a " + placeholder + " " + echo("b")));
        assertEquals(placeholder + "|y", PlaceholderParser.replacePlaceholders(join(placeholder, "y")));
    }

    @Test
    void placeholdersGetReplacedByPositionWhatEverTheirOutputContains() {
        // The output of the inner placeholder ends like a placeholder, which must not change how the outer placeholder reads its value
        TestPlaceholders.setValue("evaluation_position", "x\"}} y");
        String inner = value("evaluation_position");
        assertEquals("x\"}} y and x\"}} y", PlaceholderParser.replacePlaceholders(echo(inner) + " and " + inner));
        assertEquals("x\"}} y|z x\"}} y", PlaceholderParser.replacePlaceholders(join(inner, "z") + " " + inner));
        TestPlaceholders.setValue("evaluation_position_comma", "a\",\"second\":\"b");
        String comma = value("evaluation_position_comma");
        assertEquals("a\",\"second\":\"b|c a\",\"second\":\"b", PlaceholderParser.replacePlaceholders(join(comma, "c") + " " + comma));
    }

    @Test
    void textsStillGetTheirFormattingCodesConverted() {
        assertEquals("§ared §bblue", PlaceholderParser.replacePlaceholders(echo(echo("&ared")) + " &bblue"));
        assertEquals("&ared &bblue", PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(echo(echo("&ared")) + " &bblue"));
        assertEquals("§ared", PlaceholderParser.replacePlaceholdersWithoutLengthLimit(echo(echo("&ared")), false));
    }

    @Test
    void otherAfterProcessorsStillRunOnValues() {
        this.addProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> in.replace("$$evaluation_marker", "resolved&a"));
        PlaceholderParser.replacePlaceholders(capture("evaluation_after", echo("v") + " $$evaluation_marker"));
        assertCapturedText("evaluation_after", "v resolved&a");
        PlaceholderParser.replacePlaceholders(echo(capture("evaluation_after_deep", "$$evaluation_marker")));
        assertCapturedText("evaluation_after_deep", "resolved&a");
        assertEquals("resolved&a", PlaceholderParser.replacePlaceholders("$$evaluation_marker"));
    }

    @Test
    void placeholderStringIsTheRawTextOfThePlaceholder() {
        String placeholder = capture("evaluation_raw", echo("x  y"));
        PlaceholderParser.replacePlaceholders("a " + placeholder + " b");
        assertCapturedPlaceholderString("evaluation_raw", placeholder);
        String nested = capture("evaluation_raw_nested", "y");
        PlaceholderParser.replacePlaceholders(join(echo(nested), "z"));
        assertCapturedPlaceholderString("evaluation_raw_nested", nested);
        String spaced = "{ \"placeholder\" : \"" + capture("evaluation_raw_spaced", "s").substring(16);
        PlaceholderParser.replacePlaceholders(spaced);
        assertCapturedPlaceholderString("evaluation_raw_spaced", spaced);
    }

    @Test
    void parsedPlaceholderGetsReplacedWithItsValues() {
        TestPlaceholders.setValue("evaluation_parsed", "p");
        List<PlaceholderParser.ParsedPlaceholder> found = PlaceholderParser.findPlaceholders("a " + join(value("evaluation_parsed"), "&b") + " " + UNKNOWN, new HashMap<>());
        assertEquals(2, found.size());
        assertEquals("p|&b", found.get(0).getReplacement());
        assertEquals(UNKNOWN, found.get(1).getReplacement());
    }

    private void addProcessor(@NotNull PlaceholderParser.ParsingProcessorTiming timing, @NotNull ConsumingSupplier<String, String> processor) {
        this.processorIds.add(PlaceholderParser.addParsingProcessor(timing, processor));
    }

    private static void assertCapturedText(@NotNull String key, @NotNull String expected) {
        DeserializedPlaceholderString captured = TestPlaceholders.captured(key);
        assertNotNull(captured, key);
        assertEquals(expected, captured.values.get("text"));
    }

    private static void assertCapturedPlaceholderString(@NotNull String key, @NotNull String expected) {
        DeserializedPlaceholderString captured = TestPlaceholders.captured(key);
        assertNotNull(captured, key);
        assertEquals(expected, captured.placeholderString);
    }

}
