package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.testing.TestLanguage;
import net.minecraft.locale.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link LocalizationUtils#formatWholeNumber(long, Language)} groups digits in threes with the separator the language translates.
 */
class LocalizationUtilsWholeNumberFormattingTest {

    private static final Language COMMA = separator(",");

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0|0",
            "7|7",
            "999|999",
            "1000|1,000",
            "12345|12,345",
            "100000|100,000",
            "999999|999,999",
            "1000000|1,000,000",
            "-1|-1",
            "-999|-999",
            "-1000|-1,000",
            "-1234567|-1,234,567",
            "9223372036854775807|9,223,372,036,854,775,807",
            "-9223372036854775808|-9,223,372,036,854,775,808"
    })
    void groupsTheDigitsInThreesFromTheRight(long number, String expected) {
        assertEquals(expected, LocalizationUtils.formatWholeNumber(number, COMMA));
    }

    @Test
    void separatorComesFromTheLanguage() {
        assertEquals("100.000", LocalizationUtils.formatWholeNumber(100000L, separator(".")));
        assertEquals("100 000", LocalizationUtils.formatWholeNumber(100000L, separator(" ")));
        assertEquals("-1 234 567", LocalizationUtils.formatWholeNumber(-1234567L, separator(" ")));
        // Separators with more than one character and without any characters
        assertEquals("1::234::567", LocalizationUtils.formatWholeNumber(1234567L, separator("::")));
        assertEquals("1234567", LocalizationUtils.formatWholeNumber(1234567L, separator("")));
    }

    @Test
    void languageWithoutSeparatorTranslationUsesComma() {
        assertEquals("100,000", LocalizationUtils.formatWholeNumber(100000L, new TestLanguage(Map.of())));
    }

    private static Language separator(String separator) {
        return new TestLanguage(Map.of(LocalizationUtils.DIGIT_GROUP_SEPARATOR_KEY, separator));
    }

}
