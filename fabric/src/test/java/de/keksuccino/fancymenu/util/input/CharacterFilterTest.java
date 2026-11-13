package de.keksuccino.fancymenu.util.input;

import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    void filterKeepsAllowedCharactersInTheirOrder() {
        CharacterFilter filter = CharacterFilter.buildDigitFilter();

        assertEquals("", filter.filterForAllowedChars(""));
        assertEquals("", filter.filterForAllowedChars("abc"));
        assertEquals("123", filter.filterForAllowedChars("123"));
        assertEquals("123", filter.filterForAllowedChars("a1b2c3"));
        assertEquals("123", filter.filterForAllowedChars("123abc"));
        assertEquals("123", filter.filterForAllowedChars("abc123"));
        assertTrue(filter.isAllowedText(""));
        assertTrue(filter.isAllowedText("123"));
        assertFalse(filter.isAllowedText("12a"));
        assertFalse(filter.isAllowedText("a12"));
    }

    @Test
    void filterWithoutAllowedCharactersOnlyRemovesForbiddenOnes() {
        CharacterFilter filter = new CharacterFilter();
        filter.addForbiddenCharacters('a', '\u00e4');
        filter.addForbiddenCharacters("b", "", null, "cd");

        assertEquals("d123\u00f6", filter.filterForAllowedChars("abc\u00e4d123\u00f6"));
        assertTrue(filter.isAllowedText("d123\u00f6"));
        assertFalse(filter.isAllowedText("\u00e4"));
        assertTrue(filter.isAllowedChar('d'));
        assertFalse(filter.isAllowedChar('c'));
    }

    @Test
    void filterUsesTheCharacterCheckOfItsSubclass() {
        CharacterFilter filter = new CharacterFilter() {
            @Override
            public boolean isAllowedChar(char c) {
                return c == 'x';
            }
        };

        assertEquals("xx", filter.filterForAllowedChars("axbx"));
        assertTrue(filter.isAllowedText("xxx"));
        assertFalse(filter.isAllowedText("xax"));
    }

    @Test
    void filteringALongTextAllocatesInProportionToItsLength() {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        CharacterFilter filter = CharacterFilter.buildDigitFilter();
        // 50,000 characters stay and 50,000 get removed. Copying the result for every character that stays takes more than 1 GB.
        String mixed = "1a".repeat(50000);
        String allowed = "1".repeat(100000);
        // The first calls load all classes, which keeps their one-time allocations out of the measured calls
        filter.filterForAllowedChars("1a");
        filter.isAllowedText("1");

        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        String filtered = filter.filterForAllowedChars(mixed);
        boolean allowedText = filter.isAllowedText(allowed);
        boolean mixedText = filter.isAllowedText(mixed);
        long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;

        assertEquals("1".repeat(50000), filtered);
        assertTrue(allowedText);
        assertFalse(mixedText);
        assertTrue(allocated < 8L * 1024L * 1024L, () -> "Allocated " + allocated + " bytes for 100,000 characters");
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
