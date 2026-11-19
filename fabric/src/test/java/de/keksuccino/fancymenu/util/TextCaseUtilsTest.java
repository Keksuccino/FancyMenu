package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Case transforms keep formatting codes as they are and handle the text around them as if the codes weren't there.
 * The texts avoid the letter i where its case changes, because the transforms use the default locale, which has its own rules for i in Turkish.
 */
class TextCaseUtilsTest {

    private static final List<UnaryOperator<String>> ALL_TRANSFORMS = List.of(TextCaseUtils::toUpperCase, TextCaseUtils::toLowerCase, TextCaseUtils::toTitleCase, TextCaseUtils::toSentenceCase, TextCaseUtils::toAlternatingCase, TextCaseUtils::toToggleCase, TextCaseUtils::toSnakeCase, TextCaseUtils::toKebabCase);

    @Test
    void uppercaseKeepsFormattingCodes() {
        assertEquals("&aSTEVE", TextCaseUtils.toUpperCase("&aSteve"));
        assertEquals("§aSTEVE", TextCaseUtils.toUpperCase("§aSteve"));
        assertEquals("A&lB&rC&0D", TextCaseUtils.toUpperCase("a&lb&rc&0d"));
        assertEquals("&a&lHO", TextCaseUtils.toUpperCase("&a&lho"));
        assertEquals("ROCK&rOLL", TextCaseUtils.toUpperCase("rock&roll"));
        // Uppercase codes behind § are codes too, uppercase codes behind & are no codes and are uppercase already
        assertEquals("§LHO", TextCaseUtils.toUpperCase("§Lho"));
        assertEquals("&AHO", TextCaseUtils.toUpperCase("&Aho"));
    }

    @Test
    void uppercaseTransformsCharactersThatAreNoFormattingCodes() {
        assertEquals("&X &G &", TextCaseUtils.toUpperCase("&x &g &"));
        assertEquals("§X§", TextCaseUtils.toUpperCase("§x§"));
        // The first & has no code behind it, the second one has
        assertEquals("&&a", TextCaseUtils.toUpperCase("&&a"));
        assertEquals("&&aB", TextCaseUtils.toUpperCase("&&ab"));
        assertEquals("§§aB", TextCaseUtils.toUpperCase("§§ab"));
    }

    @Test
    void uppercaseUsesTheFullCaseMappingsBetweenFormattingCodes() {
        assertEquals("STRASSE&aSTRASSE", TextCaseUtils.toUpperCase("straße&astraße"));
        assertEquals("STRASSE", TextCaseUtils.toUpperCase("straße"));
    }

    @Test
    void lowercaseKeepsFormattingCodes() {
        assertEquals("&ahello", TextCaseUtils.toLowerCase("&aHELLO"));
        assertEquals("§Ahello§Lworld", TextCaseUtils.toLowerCase("§AHELLO§LWORLD"));
        // &A is no code, so it gets lowercased like all other text
        assertEquals("&ahello", TextCaseUtils.toLowerCase("&AHELLO"));
        assertEquals("&x", TextCaseUtils.toLowerCase("&X"));
    }

    @Test
    void titleCaseIgnoresFormattingCodes() {
        assertEquals("&aHello World", TextCaseUtils.toTitleCase("&ahello world"));
        assertEquals("Hello &lBig&r World", TextCaseUtils.toTitleCase("hello &lbig&r world"));
        assertEquals("§aHello", TextCaseUtils.toTitleCase("§aHELLO"));
        assertEquals("§AHello", TextCaseUtils.toTitleCase("§AHELLO"));
        // A code inside a word doesn't start a new word
        assertEquals("Foo&lbar", TextCaseUtils.toTitleCase("foo&lbar"));
        assertEquals("That's &aA Test", TextCaseUtils.toTitleCase("that's &aa test"));
    }

    @Test
    void sentenceCaseIgnoresFormattingCodes() {
        assertEquals("&aHello. &lWorld", TextCaseUtils.toSentenceCase("&aHELLO. &lWORLD"));
        assertEquals("§AHello! §lHo", TextCaseUtils.toSentenceCase("§AHELLO! §lHO"));
        assertEquals("&aHello", TextCaseUtils.toSentenceCase("&AHELLO"));
    }

    @Test
    void alternatingCaseIgnoresFormattingCodes() {
        assertEquals("&ahElLo", TextCaseUtils.toAlternatingCase("&aHello"));
        assertEquals("hE&llLo", TextCaseUtils.toAlternatingCase("he&lllo"));
        assertEquals("§LhE§r", TextCaseUtils.toAlternatingCase("§Lhe§r"));
    }

    @Test
    void toggleCaseIgnoresFormattingCodes() {
        assertEquals("hELLO &awORLD", TextCaseUtils.toToggleCase("Hello &aWorld"));
        assertEquals("§Ab§l", TextCaseUtils.toToggleCase("§AB§l"));
        assertEquals("&aX", TextCaseUtils.toToggleCase("&Ax"));
    }

    @Test
    void snakeAndKebabCaseLeaveOutFormattingCodes() {
        assertEquals("hello_world", TextCaseUtils.toSnakeCase("&aHello World"));
        assertEquals("hello-world", TextCaseUtils.toKebabCase("&aHello World"));
        assertEquals("my_server", TextCaseUtils.toSnakeCase("§lMy §aServer§r"));
        assertEquals("hello", TextCaseUtils.toSnakeCase("§AHello"));
        assertEquals("", TextCaseUtils.toSnakeCase("&a&l"));
    }

    @Test
    void snakeAndKebabCaseSplitWordsAsIfFormattingCodesWerentThere() {
        assertEquals("helloworld", TextCaseUtils.toSnakeCase("hello&aworld"));
        assertEquals("helloworld", TextCaseUtils.toSnakeCase("HELLO&aWORLD"));
        assertEquals("my_big_value", TextCaseUtils.toSnakeCase("myBig&lValue"));
        assertEquals("abc-def", TextCaseUtils.toKebabCase("ABC&dDef"));
        assertEquals("abc-def", TextCaseUtils.toKebabCase("ABC&d&lDef"));
        assertEquals("version_2_beta", TextCaseUtils.toSnakeCase("version&a2Beta"));
    }

    @Test
    void textsWithoutFormattingCodesGetTransformedAsBefore() {
        assertEquals("HELLO WORLD", TextCaseUtils.toUpperCase("Hello World"));
        assertEquals("hello world", TextCaseUtils.toLowerCase("Hello World"));
        assertEquals("Hello World", TextCaseUtils.toTitleCase("hello WORLD"));
        assertEquals("Don't Stop", TextCaseUtils.toTitleCase("don't stop"));
        assertEquals("Hello. World? Yes", TextCaseUtils.toSentenceCase("hello. WORLD? yes"));
        assertEquals("hElLo WoRlD", TextCaseUtils.toAlternatingCase("Hello World"));
        assertEquals("hELLO wORLD", TextCaseUtils.toToggleCase("Hello World"));
        assertEquals("hello_world", TextCaseUtils.toSnakeCase("helloWorld"));
        assertEquals("xml-http-request", TextCaseUtils.toKebabCase("XMLHttpRequest"));
        assertEquals("version_2_beta", TextCaseUtils.toSnakeCase("version2Beta"));
        assertEquals("r_d", TextCaseUtils.toSnakeCase("R&D"));
    }

    @Test
    void emptyTextStaysEmpty() {
        for (UnaryOperator<String> transform : ALL_TRANSFORMS) {
            assertEquals("", transform.apply(""));
        }
    }

    @Test
    void textsOfOnlyFormattingCodesStayAsTheyAreExceptForSnakeAndKebabCase() {
        for (UnaryOperator<String> transform : ALL_TRANSFORMS.subList(0, 6)) {
            assertEquals("&a§L&r", transform.apply("&a§L&r"));
        }
    }

}
