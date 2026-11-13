package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFontCoverageTest {

    @Test
    void bitmapFontHasTheCharactersOfItsRows() {
        MarkdownFontCoverage coverage = coverage("{\"providers\":[{\"type\":\"bitmap\",\"file\":\"test:font.png\",\"ascent\":7,\"chars\":[\"ab\\u0000\",\"cd\uD83D\uDE00\"]}]}");

        assertTrue(coverage.has('a'));
        assertTrue(coverage.has('d'));
        assertTrue(coverage.has(0x1F600));
        assertFalse(coverage.has('e'));
        // The character that marks an empty slot of the texture is no character of the font
        assertFalse(coverage.has(0));
        assertTrue(coverage.hasAll("abcd"));
        assertTrue(coverage.hasAll(""));
        assertFalse(coverage.hasAll("abe"));
    }

    @Test
    void spaceProviderAddsTheCharactersItGivesAnAdvance() {
        MarkdownFontCoverage coverage = coverage("{\"providers\":[{\"type\":\"space\",\"advances\":{\" \":4,\"\\u200c\":0}},{\"type\":\"minecraft:bitmap\",\"chars\":[\"a\"]}]}");

        assertTrue(coverage.has(' '));
        assertTrue(coverage.has(0x200C));
        assertTrue(coverage.has('a'));
        assertFalse(coverage.has('b'));
    }

    @Test
    void definitionsOfAllResourcePacksCount() {
        MarkdownFontCoverage coverage = MarkdownFontCoverage.fromDefinitions(List.of(definition("{\"providers\":[{\"type\":\"bitmap\",\"chars\":[\"a\"]}]}"), definition("{\"providers\":[{\"type\":\"bitmap\",\"chars\":[\"b\"]}]}")));

        assertTrue(coverage.hasAll("ab"));
        assertFalse(coverage.has('c'));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"providers\":[{\"type\":\"bitmap\",\"chars\":[\"a\"]},{\"type\":\"ttf\",\"file\":\"test:font.ttf\"}]}",
            "{\"providers\":[{\"type\":\"reference\",\"id\":\"minecraft:default\"}]}",
            "{\"providers\":[{\"type\":\"unihex\",\"hex_file\":\"test:font.zip\"}]}",
            "{\"providers\":[{\"type\":\"legacy_unicode\",\"sizes\":\"minecraft:font/glyph_sizes.bin\",\"template\":\"minecraft:font/unicode_page_%s.png\"}]}"
    })
    void fontWithAProviderThatDoesNotListItsCharactersHasEveryCharacter(String definition) {
        MarkdownFontCoverage coverage = coverage(definition);

        assertSame(MarkdownFontCoverage.EVERYTHING, coverage);
        assertTrue(coverage.has('z'));
        assertTrue(coverage.hasAll("anything \u00E4\u4E2D"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"providers\":[{\"chars\":[\"a\"]}]}",
            "{\"providers\":[{\"type\":\"bitmap\"}]}",
            "{\"providers\":{\"type\":\"bitmap\"}}",
            "{\"providers\":[\"bitmap\"]}"
    })
    void fontWithADefinitionThatCanNotGetReadHasEveryCharacter(String definition) {
        assertSame(MarkdownFontCoverage.EVERYTHING, coverage(definition));
    }

    @Test
    void fontWithoutProvidersHasNoCharacters() {
        assertFalse(MarkdownFontCoverage.fromDefinitions(List.of()).has('a'));
        assertFalse(coverage("{}").has('a'));
        assertFalse(coverage("{\"providers\":[]}").has(' '));
    }

    private static MarkdownFontCoverage coverage(String definition) {
        return MarkdownFontCoverage.fromDefinitions(List.of(definition(definition)));
    }

    private static JsonObject definition(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

}
