package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reading the identifier and values of placeholders: value content is kept exactly as it is at every nesting depth,
 * while structural whitespace between the tokens of a placeholder gets ignored.
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserValuesTest {

    private static final String JOIN = "{\"placeholder\":\"" + TestPlaceholders.joinIdentifier() + "\"";

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
    void spacesInValuesAreKeptAtEveryNestingDepth() {
        assertEquals("hello big world", PlaceholderParser.replacePlaceholders(echo("hello big world")));
        assertEquals("hello big world", PlaceholderParser.replacePlaceholders(echo(echo("hello big world"))));
        assertEquals("a  b\tc", PlaceholderParser.replacePlaceholders(echo(echo(echo("a  b\tc")))));
        assertEquals("a b|c d", PlaceholderParser.replacePlaceholders(join(echo("a b"), echo(echo("c d")))));
    }

    @Test
    void lineBreakCodesInValuesAreKeptAtEveryNestingDepth() {
        assertEquals("a%n%b", PlaceholderParser.replacePlaceholders(echo("a%n%b")));
        assertEquals("a%n%b", PlaceholderParser.replacePlaceholders(echo(echo("a%n%b"))));
        assertEquals("a %n% b", PlaceholderParser.replacePlaceholders(echo(echo(echo("a %n% b")))));
    }

    @Test
    void spacesBetweenQuotesInValuesAreKept() {
        assertEquals("say \"hi there\" now", PlaceholderParser.replacePlaceholders(echo("say \"hi there\" now")));
        assertEquals("say \"hi there\" now", PlaceholderParser.replacePlaceholders(echo(echo("say \"hi there\" now"))));
    }

    @Test
    void structuralWhitespaceBetweenTokensIsIgnored() {
        String text = JOIN + " ,%n% \"values\" : {\n\t\"first\" : \"a b\" ,%n% \"second\":%n%\"c d\" \r\n} }";
        assertEquals("a b|c d", PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void loneOpeningBraceInValueKeepsAllValues() {
        assertEquals("x { y|z", PlaceholderParser.replacePlaceholders(join("x { y", "z")));
        assertEquals("x|{", PlaceholderParser.replacePlaceholders(join("x", "{")));
        assertEquals("{ {|{", PlaceholderParser.replacePlaceholders(join("{ {", "{")));
    }

    @Test
    void loneOpeningBraceInNestedValueIsKept() {
        assertEquals("x { y", PlaceholderParser.replacePlaceholders(echo(echo("x { y"))));
        assertEquals("x { y|z", PlaceholderParser.replacePlaceholders(join(echo("x { y"), "z")));
    }

    @Test
    void loneClosingBraceInValueIsKept() {
        assertEquals("x } y|z", PlaceholderParser.replacePlaceholders(join("x } y", "z")));
    }

    @Test
    void quotesInsideBraceBlocksOfValuesDoNotEndTheValue() {
        String json = "{\"first\":\"1\",\"second\":\"2\"}";
        assertEquals(json + "|z", PlaceholderParser.replacePlaceholders(join(json, "z")));
        assertEquals("a|b|c", PlaceholderParser.replacePlaceholders(join(join("a", "b"), "c")));
        assertEquals("a|b|c|d", PlaceholderParser.replacePlaceholders(join(join("a", "b"), join("c", "d"))));
    }

    @Test
    void valuesCanStandInAnyOrderAndUnknownValueNamesAreIgnored() {
        assertEquals("a|b", PlaceholderParser.replacePlaceholders(JOIN + ",\"values\":{\"second\":\"b\",\"first\":\"a\"}}"));
        assertEquals("a|b", PlaceholderParser.replacePlaceholders(JOIN + ",\"values\":{\"other\":\"x\",\"first\":\"a\",\"second\":\"b\"}}"));
        assertEquals("a|null", PlaceholderParser.replacePlaceholders(JOIN + ",\"values\":{\"first\":\"a\"}}"));
    }

    @Test
    void emptyAndEscapedValuesAreKeptAsTheyAre() {
        assertEquals("|z", PlaceholderParser.replacePlaceholders(join("", "z")));
        assertEquals("a\\\"b|z", PlaceholderParser.replacePlaceholders(join("a\\\"b", "z")));
        assertEquals("a\\{b|z", PlaceholderParser.replacePlaceholders(join("a\\{b", "z")));
    }

    @Test
    void valueWithoutEndIsMissing() {
        Map<String, String> values = parse(JOIN + ",\"values\":{\"first\":\"a\" x}}").getValues();
        assertNotNull(values);
        assertNull(values.get("first"));
        assertEquals(Map.of(), parse(JOIN + "}").getValues());
    }

    @Test
    void valuesOfParsedPlaceholderAreTheRawValueTexts() {
        PlaceholderParser.ParsedPlaceholder parsed = parse(join(echo("a  b"), "c%n%d"));
        assertEquals(TestPlaceholders.joinIdentifier(), parsed.getIdentifier());
        Map<String, String> values = parsed.getValues();
        assertNotNull(values);
        assertEquals(echo("a  b"), values.get("first"));
        assertEquals("c%n%d", values.get("second"));
    }

    @Test
    void identifierGetsReadAroundStructuralWhitespace() {
        assertEquals(TestPlaceholders.echoIdentifier(), parse("{ \"placeholder\" :%n% \"" + TestPlaceholders.echoIdentifier() + "\" , \"values\":{\"text\":\"x\"}}").getIdentifier());
        assertEquals(" spaced id ", parse("{\"placeholder\":\" spaced id \"}").getIdentifier());
        assertNull(parse("{\"placeholder\":\"\"}").getIdentifier());
    }

    @Test
    void unknownPlaceholderHasNoValuesAndStaysAsItIs() {
        String text = "{\"placeholder\":\"values_test_unknown\",\"values\":{\"text\":\"x\"}}";
        PlaceholderParser.ParsedPlaceholder parsed = parse(text);
        assertEquals("values_test_unknown", parsed.getIdentifier());
        assertNull(parsed.getPlaceholder());
        assertNull(parsed.getValues());
        assertEquals(text, PlaceholderParser.replacePlaceholders(text));
    }

    @Test
    void placeholderCreatedBySubclassReadsItsOwnString() {
        String text = "{ \"placeholder\" : \"" + TestPlaceholders.echoIdentifier() + "\", \"values\": { \"text\": \"sub class\" } }";
        PlaceholderParser.ParsedPlaceholder parsed = new PlaceholderParser.ParsedPlaceholder(text, 0, text.length(), new HashMap<>(), false) {};
        assertEquals(TestPlaceholders.echoIdentifier(), parsed.getIdentifier());
        assertEquals(Map.of("text", "sub class"), parsed.getValues());
        assertEquals("sub class", parsed.getReplacement());
        PlaceholderParser.ParsedPlaceholder noPlaceholder = new PlaceholderParser.ParsedPlaceholder("not a placeholder", 0, 17, new HashMap<>(), false) {};
        assertNull(noPlaceholder.getIdentifier());
        assertNull(noPlaceholder.getPlaceholder());
    }

    @Test
    void placeholderCreatedBySubclassSkipsStructuralWhitespaceBeforeItsStart() {
        String placeholder = "{\"placeholder\":\"" + TestPlaceholders.echoIdentifier() + "\",\"values\":{\"text\":\"x  y\"}}";
        for (String text : List.of("%n% " + placeholder, " \t\n\r" + placeholder, "%n%%n%" + placeholder + " %n%", "%n%{ \"placeholder\" : \"" + TestPlaceholders.echoIdentifier() + "\" , \"values\" : { \"text\" : \"x  y\" } }")) {
            PlaceholderParser.ParsedPlaceholder parsed = new PlaceholderParser.ParsedPlaceholder(text, 0, text.length(), new HashMap<>(), false) {};
            assertEquals(TestPlaceholders.echoIdentifier(), parsed.getIdentifier(), text);
            assertNotNull(parsed.getPlaceholder(), text);
            assertEquals(Map.of("text", "x  y"), parsed.getValues(), text);
            assertEquals("x  y", parsed.getReplacement(), text);
        }
    }

    @Test
    void placeholderCreatedBySubclassIsNoPlaceholderWithOtherTextBeforeItsStart() {
        for (String text : List.of("x{\"placeholder\":\"" + TestPlaceholders.echoIdentifier() + "\",\"values\":{\"text\":\"x\"}}", "%n", "%n% ", "")) {
            PlaceholderParser.ParsedPlaceholder parsed = new PlaceholderParser.ParsedPlaceholder(text, 0, text.length(), new HashMap<>(), false) {};
            assertNull(parsed.getIdentifier(), text);
            assertNull(parsed.getPlaceholder(), text);
            assertNull(parsed.getValues(), text);
            assertEquals(text, parsed.getReplacement(), text);
        }
    }

    @NotNull
    private static PlaceholderParser.ParsedPlaceholder parse(@NotNull String text) {
        List<PlaceholderParser.ParsedPlaceholder> found = PlaceholderParser.findPlaceholders(text, new HashMap<>());
        assertEquals(1, found.size(), text);
        return found.get(0);
    }

}
