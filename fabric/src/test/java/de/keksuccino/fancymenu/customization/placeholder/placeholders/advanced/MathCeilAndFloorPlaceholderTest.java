package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MathCeilAndFloorPlaceholderTest {

    private static final MathCeilPlaceholder CEILING = new MathCeilPlaceholder();
    private static final MathFloorPlaceholder FLOOR = new MathFloorPlaceholder();

    @Test
    void formatsPositiveAndNegativeRoundedResultsAsIntegers() {
        assertReplacement(CEILING, "3.14", "4");
        assertReplacement(CEILING, "-3.14", "-3");
        assertReplacement(FLOOR, "3.14", "3");
        assertReplacement(FLOOR, "-3.14", "-4");
    }

    @Test
    void removesDecimalSuffixFromAlreadyWholeInputs() {
        assertReplacement(CEILING, "42.0", "42");
        assertReplacement(CEILING, "-42.0", "-42");
        assertReplacement(FLOOR, "42.0", "42");
        assertReplacement(FLOOR, "-42.0", "-42");
    }

    @Test
    void normalizesSignedZeroAndSubnormalResults() {
        assertReplacement(CEILING, "0.0", "0");
        assertReplacement(CEILING, "-0.0", "0");
        assertReplacement(FLOOR, "0.0", "0");
        assertReplacement(FLOOR, "-0.0", "0");
        assertReplacement(CEILING, Double.toString(-Double.MIN_VALUE), "0");
        assertReplacement(FLOOR, Double.toString(Double.MIN_VALUE), "0");
        assertReplacement(CEILING, Double.toString(Double.MIN_VALUE), "1");
        assertReplacement(FLOOR, Double.toString(-Double.MIN_VALUE), "-1");
    }

    @Test
    void formatsFiniteResultsBeyondTheLongRangeWithoutOverflow() {
        String maxDoubleInteger = BigInteger.ONE.shiftLeft(1024).subtract(BigInteger.ONE.shiftLeft(971)).toString();

        assertReplacement(CEILING, "0x1.0p63", "9223372036854775808");
        assertReplacement(FLOOR, "0x1.0000000000001p63", "9223372036854777856");
        assertReplacement(CEILING, Double.toString(Double.MAX_VALUE), maxDoubleInteger);
        assertReplacement(FLOOR, Double.toString(Double.MAX_VALUE), maxDoubleInteger);
        assertReplacement(CEILING, Double.toString(-Double.MAX_VALUE), "-" + maxDoubleInteger);
        assertReplacement(FLOOR, Double.toString(-Double.MAX_VALUE), "-" + maxDoubleInteger);
    }

    @Test
    void preservesExistingNonFiniteResultSpellings() {
        for (String value : new String[]{"NaN", "Infinity", "-Infinity"}) {
            assertReplacement(CEILING, value, value);
            assertReplacement(FLOOR, value, value);
        }
    }

    @Test
    void preservesDoubleParsingRules() {
        assertReplacement(CEILING, " 3.14 ", "4");
        assertReplacement(FLOOR, " 3.14 ", "3");
        assertReplacement(CEILING, "0x1.8p1", "3");
        assertReplacement(FLOOR, "0x1.8p1", "3");
    }

    @Test
    void returnsNullForMissingOrInvalidInputs() {
        for (Placeholder placeholder : new Placeholder[]{CEILING, FLOOR}) {
            assertNull(replacement(placeholder, null));
            assertNull(replacement(placeholder, ""));
            assertNull(replacement(placeholder, "not-a-number"));
        }
    }

    private static void assertReplacement(Placeholder placeholder, String input, String expected) {
        assertEquals(expected, replacement(placeholder, input));
    }

    private static String replacement(Placeholder placeholder, String input) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        if (input != null) values.put("num", input);
        String placeholderString = new DeserializedPlaceholderString(placeholder.getIdentifier(), values, "").toString();
        return placeholder.getReplacementFor(new DeserializedPlaceholderString(placeholder.getIdentifier(), values, placeholderString));
    }

}
