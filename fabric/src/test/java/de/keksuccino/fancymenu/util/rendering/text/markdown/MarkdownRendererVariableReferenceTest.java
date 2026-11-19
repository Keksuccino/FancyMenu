package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code $$name} references in the text of a renderer itself, outside of its placeholders, have to update the text when the variables change.
 *
 * <p>The references get resolved by a test processor here, which runs at the same point as the processor for FancyMenu variables, and the
 * renderer sees the revision of these test variables, so the tests don't need the global variable store, which writes to disk.</p>
 */
@ResourceLock("PlaceholderParser global state")
class MarkdownRendererVariableReferenceTest {

    private static final String MARKER = "mrv_";

    private final Map<String, String> variables = new LinkedHashMap<>();
    /** How many texts with a test variable name the test processor resolved, which is how often the renderer built its render text. */
    private final AtomicInteger resolvedTexts = new AtomicInteger();
    /** Runs once, right after the test processor resolved the next text. */
    @Nullable
    private Runnable afterNextResolve;
    private long processorId;
    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void setUp() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        this.processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> {
            if (!in.contains(MARKER)) return in;
            this.resolvedTexts.incrementAndGet();
            // Like FancyMenu's variables: the longest name wins at every $$
            String resolved = ShortReferenceResolver.resolve(in, (text, nameStart) -> {
                String name = ShortReferenceResolver.findLongestName(text, nameStart, this.variables.keySet(), n -> n);
                return (name != null) ? new ShortReferenceResolver.Reference(name.length(), this.variables.get(name)) : null;
            });
            Runnable after = this.afterNextResolve;
            this.afterNextResolve = null;
            if (after != null) after.run();
            return resolved;
        });
    }

    @AfterEach
    void restoreGlobalState() {
        PlaceholderParser.removeParsingProcessor(this.processorId);
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void changedVariableBuildsAndParsesTheTextAgain() {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Coins: $$mrv_coins", 400);
        renderer.tick();
        assertEquals("Coins: 1", renderer.renderText);

        this.changeVariable(renderer, "mrv_coins", "2");
        renderer.tick();

        assertAll(() -> assertEquals("Coins: 2", renderer.renderText), () -> assertEquals("2", renderer.fragmentContaining("2").text), () -> assertEquals(2, renderer.parses));
    }

    @Test
    void addedAndRemovedVariablesChangeWhichNameWins() {
        this.variables.put("mrv_c", "C");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Record: $$mrv_c_record!", 400);
        renderer.tick();
        assertEquals("Record: C_record!", renderer.renderText);

        this.changeVariable(renderer, "mrv_c_record", "80");
        renderer.tick();
        assertEquals("Record: 80!", renderer.renderText);

        this.removeVariable(renderer, "mrv_c_record");
        renderer.tick();
        assertEquals("Record: C_record!", renderer.renderText);

        this.removeVariable(renderer, "mrv_c");
        renderer.tick();
        assertAll(() -> assertEquals("Record: $$mrv_c_record!", renderer.renderText), () -> assertEquals(4, renderer.parses));
    }

    @Test
    void unchangedVariablesNeverBuildTheTextAgain() {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Coins: $$mrv_coins", 400);

        for (int i = 0; i < 5; i++) {
            renderer.tick();
            renderer.getRealHeight();
            renderer.getRealWidth();
        }

        // The revision gets read once per frame, and nothing else happens while it stays the same
        assertAll(() -> assertEquals(5, renderer.variablesRevisionReads), () -> assertEquals(1, this.resolvedTexts.get()), () -> assertEquals(1, renderer.parses), () -> assertEquals(1, renderer.layouts));
    }

    @Test
    void changeThatResolvesToTheSameTextBuildsTheTextButDoesNotParseIt() {
        this.variables.put("mrv_coins", "1");
        this.variables.put("mrv_other", "a");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Coins: $$mrv_coins", 400);
        renderer.tick();

        this.changeVariable(renderer, "mrv_other", "b");
        renderer.tick();

        assertAll(() -> assertEquals(2, this.resolvedTexts.get()), () -> assertEquals("Coins: 1", renderer.renderText), () -> assertEquals(1, renderer.parses), () -> assertEquals(1, renderer.layouts));
    }

    @Test
    void textsWithoutReferencesOfTheirOwnNeverReadTheRevision() {
        this.variables.put("mrv_coins", "1");
        List<String> texts = List.of("plain text", "price: 5 $", "a $ b $", "ends with $$", "$$", TestPlaceholders.echo("value") + " $ " + TestPlaceholders.echo("value"), "Coins: " + TestPlaceholders.echo("$$mrv_coins"));
        for (String text : texts) {
            MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
            renderer.tick();
            renderer.variablesRevision++;
            renderer.tick();
            renderer.tick();
            assertEquals(0, renderer.variablesRevisionReads, text);
        }
    }

    @Test
    void referenceInsidePlaceholderGetsNoticedThroughThePlaceholder() {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Coins: " + TestPlaceholders.echo("$$mrv_coins"), 400);
        renderer.tick();
        assertEquals("Coins: 1", renderer.renderText);

        // Without a new revision: the placeholders of the text get replaced every frame, which resolves the reference in their value
        this.variables.put("mrv_coins", "2");
        renderer.tick();

        assertAll(() -> assertEquals("Coins: 2", renderer.renderText), () -> assertEquals(0, renderer.variablesRevisionReads));
    }

    /**
     * @return The text, and the render text it should get with {@code %s} for the value of the variable.
     */
    static Stream<Arguments> textsWithReferencesOfTheirOwn() {
        return Stream.of(
                Arguments.of("# Coins\n\n$$mrv_coins coins", "# Coins\n\n%s coins"),
                Arguments.of("$$mrv_coins" + TestPlaceholders.echo(" coins"), "%s coins"),
                // The name comes from the output of the placeholder
                Arguments.of("$$" + TestPlaceholders.echo("mrv_coins") + " coins", "%s coins"),
                // The output of the placeholder adds the other $ of the reference
                Arguments.of("$" + TestPlaceholders.echo("$mrv_coins") + " coins", "%s coins"),
                Arguments.of(TestPlaceholders.echo("$") + "$mrv_coins coins", "%s coins")
        );
    }

    @ParameterizedTest
    @MethodSource("textsWithReferencesOfTheirOwn")
    void referencesThatStartInTheTextItselfFollowTheVariables(String text, String expected) {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.tick();
        assertEquals(String.format(expected, "1"), renderer.renderText);

        this.changeVariable(renderer, "mrv_coins", "2");
        renderer.tick();

        assertAll(() -> assertEquals(String.format(expected, "2"), renderer.renderText), () -> assertEquals(2, renderer.variablesRevisionReads));
    }

    @Test
    void newTextDecidesAgainIfTheVariablesGetWatched() {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("$$mrv_coins", 400);
        renderer.tick();
        assertEquals(1, renderer.variablesRevisionReads);

        renderer.setText("plain text");
        renderer.tick();
        renderer.tick();
        assertEquals(1, renderer.variablesRevisionReads);

        // The variable changed while the renderer didn't watch it, so the next text with the reference has to show the current value
        this.changeVariable(renderer, "mrv_coins", "2");
        renderer.setText("$$mrv_coins!");
        renderer.tick();
        assertAll(() -> assertEquals("2!", renderer.renderText), () -> assertEquals(2, renderer.variablesRevisionReads));

        // Texts that are too long get replaced with an error text, which has no references
        renderer.setText("$$mrv_coins " + "x".repeat(45001));
        renderer.tick();
        renderer.tick();
        assertEquals(2, renderer.variablesRevisionReads);
    }

    @Test
    void variableChangeWhileTheTextGetsBuiltGetsNoticedTheNextFrame() {
        this.variables.put("mrv_coins", "1");
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("Coins: $$mrv_coins", 400);
        renderer.tick();

        this.changeVariable(renderer, "mrv_coins", "2");
        // Acts like a script on another thread that changes the variable right after the parser read it
        this.afterNextResolve = () -> this.changeVariable(renderer, "mrv_coins", "3");
        renderer.tick();
        assertEquals("Coins: 2", renderer.renderText);

        renderer.tick();
        assertEquals("Coins: 3", renderer.renderText);
    }

    private void changeVariable(@NotNull MarkdownTestRenderer renderer, @NotNull String name, @NotNull String value) {
        this.variables.put(name, value);
        renderer.variablesRevision++;
    }

    private void removeVariable(@NotNull MarkdownTestRenderer renderer, @NotNull String name) {
        this.variables.remove(name);
        renderer.variablesRevision++;
    }

}
