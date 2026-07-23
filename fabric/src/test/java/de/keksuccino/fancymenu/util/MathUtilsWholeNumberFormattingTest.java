package de.keksuccino.fancymenu.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathUtilsWholeNumberFormattingTest {

    @Test
    void formatsFiniteWholeNumbersWithoutDecimalSuffixes() {
        assertEquals("42", MathUtils.formatWholeNumber(42.0D));
        assertEquals("-42", MathUtils.formatWholeNumber(-42.0D));
        assertEquals("0", MathUtils.formatWholeNumber(0.0D));
        assertEquals("0", MathUtils.formatWholeNumber(-0.0D));
    }

    @Test
    void formatsWholeNumbersBeyondTheLongRangeWithoutOverflow() {
        String maxDoubleInteger = BigInteger.ONE.shiftLeft(1024).subtract(BigInteger.ONE.shiftLeft(971)).toString();

        assertEquals("9223372036854775808", MathUtils.formatWholeNumber(0x1.0p63));
        assertEquals("9223372036854777856", MathUtils.formatWholeNumber(Math.nextUp(0x1.0p63)));
        assertEquals(maxDoubleInteger, MathUtils.formatWholeNumber(Double.MAX_VALUE));
        assertEquals("-" + maxDoubleInteger, MathUtils.formatWholeNumber(-Double.MAX_VALUE));
    }

    @Test
    void preservesCanonicalTextForFractionalAndNonFiniteValues() {
        assertEquals("3.5", MathUtils.formatWholeNumber(3.5D));
        assertEquals("NaN", MathUtils.formatWholeNumber(Double.NaN));
        assertEquals("Infinity", MathUtils.formatWholeNumber(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", MathUtils.formatWholeNumber(Double.NEGATIVE_INFINITY));
    }

}
