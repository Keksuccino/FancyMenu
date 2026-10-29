package de.keksuccino.fancymenu.util.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterFilterTest {

    private static final String DIGITS = "0123456789";

    @Test
    void digitFilterAllowsOnlyDigits() {
        CharacterFilter filter = CharacterFilter.buildDigitFilter();
        for (char c : DIGITS.toCharArray()) {
            assertTrue(filter.isAllowedChar(c), "digit " + c);
        }
        for (char c : "-+.,a Z\t".toCharArray()) {
            assertFalse(filter.isAllowedChar(c), "char " + (int) c);
        }
        assertEquals("1234", filter.filterForAllowedChars("-1+2.3a4"));
    }

    @Test
    void integerFilterAllowsDigitsAndSigns() {
        CharacterFilter filter = CharacterFilter.buildIntegerFilter();
        for (char c : (DIGITS + "-+").toCharArray()) {
            assertTrue(filter.isAllowedChar(c), "char " + c);
        }
        for (char c : ".,a ".toCharArray()) {
            assertFalse(filter.isAllowedChar(c), "char " + (int) c);
        }
    }

    @Test
    void builtFiltersAreIndependentInstances() {
        CharacterFilter integerFilter = CharacterFilter.buildIntegerFilter();
        CharacterFilter digitFilter = CharacterFilter.buildDigitFilter();
        integerFilter.addAllowedCharacters("x");
        assertFalse(digitFilter.isAllowedChar('-'));
        assertFalse(digitFilter.isAllowedChar('x'));
        assertFalse(CharacterFilter.buildDigitFilter().isAllowedChar('x'));
    }

}
