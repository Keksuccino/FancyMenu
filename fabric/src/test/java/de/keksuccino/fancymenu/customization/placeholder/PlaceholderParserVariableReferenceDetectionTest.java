package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code $$name} references get resolved by a processor that runs after placeholders got replaced,
 * so the parser has to treat texts with such a reference like texts with placeholders.
 *
 * <p>The references get resolved by a test processor here, which runs at the same point as the processor for FancyMenu variables,
 * so the tests don't need the global variable store, which writes to disk.</p>
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserVariableReferenceDetectionTest {

    private static final String MARKER = "parser_detection";
    private static final String COINS_REFERENCE = "$$parser_detection_coins";
    private static final String KEY_REFERENCE = "$$parser_detection_key";
    private static final String RESOLVED_KEY = "parser_detection_resolved_key";
    private static final String SHORT_REFERENCE = "$$pd";

    private final AtomicInteger processedTexts = new AtomicInteger();
    private long processorId;
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void setUp() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        this.processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> {
            if (in.contains(MARKER) || in.contains(SHORT_REFERENCE)) this.processedTexts.incrementAndGet();
            return in.replace(COINS_REFERENCE, "25").replace(KEY_REFERENCE, RESOLVED_KEY).replace(SHORT_REFERENCE, "ok");
        });
    }

    @AfterEach
    void restoreGlobalState() {
        PlaceholderParser.removeParsingProcessor(this.processorId);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void referenceGetsResolvedInTextWithoutPlaceholders() {
        assertEquals("Coins: 25", PlaceholderParser.replacePlaceholders("Coins: " + COINS_REFERENCE));
        assertEquals("25", PlaceholderParser.replacePlaceholders(COINS_REFERENCE));
    }

    @Test
    void referenceGetsResolvedInTextShorterThanTheShortestPlaceholder() {
        assertEquals("ok", PlaceholderParser.replacePlaceholders(SHORT_REFERENCE));
    }

    @Test
    void referenceGetsResolvedInTextWithPlaceholders() {
        assertEquals("Coins: 25 echo", PlaceholderParser.replacePlaceholders("Coins: " + COINS_REFERENCE + " " + TestPlaceholders.echo("echo")));
    }

    @Test
    void referenceGetsResolvedByEveryParsingVariant() {
        assertEquals("&aCoins: 25", PlaceholderParser.replacePlaceholdersPreservingFormattingCodes("&aCoins: " + COINS_REFERENCE));
        assertEquals("Coins: 25", PlaceholderParser.replacePlaceholdersWithoutLengthLimit("Coins: " + COINS_REFERENCE, false));
    }

    @Test
    void referenceInPlaceholderValueGetsResolvedBeforeThePlaceholderUsesIt() {
        TestPlaceholders.setValue(RESOLVED_KEY, "found");
        assertEquals("found", PlaceholderParser.replacePlaceholders(TestPlaceholders.value(KEY_REFERENCE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {MARKER + " plain text", MARKER + " costs 5$ or $6", MARKER + " ends with $$"})
    void textWithoutPlaceholdersOrReferencesSkipsProcessing(String text) {
        assertEquals(text, PlaceholderParser.replacePlaceholders(text));
        assertEquals(0, this.processedTexts.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"$$a", "$$$", "Coins: $$coins", "$$coins and more text", "{\"placeholder\":\"playername\"}"})
    void textWithPlaceholdersOrReferencesContainsPlaceholders(String text) {
        assertTrue(PlaceholderParser.containsPlaceholders(text));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"$", "$$", "a$$", "Costs 5$ or $6", "Ends with $$", "Plain text without anything"})
    void textWithoutPlaceholdersOrReferencesContainsNoPlaceholders(String text) {
        assertFalse(PlaceholderParser.containsPlaceholders(text));
    }

}
