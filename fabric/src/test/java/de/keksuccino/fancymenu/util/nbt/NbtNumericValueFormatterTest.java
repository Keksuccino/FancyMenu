package de.keksuccino.fancymenu.util.nbt;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NbtNumericValueFormatterTest {

    @ParameterizedTest
    @MethodSource("unitScaleValues")
    void unitScalePreservesExactTypedSnbt(NumericTag tag, String expected) {
        assertEquals(expected, NbtNumericValueFormatter.format(tag, 1.0D));
    }

    @ParameterizedTest
    @MethodSource("scaledValues")
    void scaledValuesRetainSourceTypeAndIntegralRounding(NumericTag tag, double scale, String expected) {
        assertEquals(expected, NbtNumericValueFormatter.format(tag, scale));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteFormatScalesFallBackToExactUnitScale(double scale) {
        assertEquals("9223372036854775807L", NbtNumericValueFormatter.format(LongTag.valueOf(Long.MAX_VALUE), scale));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-number", "NaN", "Infinity", "-Infinity"})
    void invalidRequiredScalesUseDefault(String scale) {
        assertEquals(NbtNumericValueFormatter.DEFAULT_SCALE, NbtNumericValueFormatter.parseScale(scale));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-number", "NaN", "Infinity", "-Infinity"})
    void invalidOptionalScalesAreAbsent(String scale) {
        assertNull(NbtNumericValueFormatter.parseOptionalScale(scale));
    }

    @Test
    void finiteScaleParsingTrimsInputAndPreservesValue() {
        assertEquals(-2.5D, NbtNumericValueFormatter.parseScale("  -2.5  "));
        assertEquals(-2.5D, NbtNumericValueFormatter.parseOptionalScale("  -2.5  "));
    }

    private static Stream<Arguments> unitScaleValues() {
        return Stream.of(
                Arguments.of(ByteTag.valueOf((byte) -12), "-12b"),
                Arguments.of(ShortTag.valueOf((short) -1234), "-1234s"),
                Arguments.of(IntTag.valueOf(Integer.MIN_VALUE), "-2147483648"),
                Arguments.of(LongTag.valueOf(Long.MAX_VALUE), "9223372036854775807L"),
                Arguments.of(FloatTag.valueOf(1.25F), "1.25f"),
                Arguments.of(DoubleTag.valueOf(-1.25D), "-1.25d")
        );
    }

    private static Stream<Arguments> scaledValues() {
        return Stream.of(
                Arguments.of(ByteTag.valueOf((byte) 5), 2.0D, "10b"),
                Arguments.of(ShortTag.valueOf((short) 6), 2.0D, "12s"),
                Arguments.of(IntTag.valueOf(7), 0.5D, "4"),
                Arguments.of(IntTag.valueOf(7), 0.0D, "0"),
                Arguments.of(LongTag.valueOf(8L), -2.0D, "-16L"),
                Arguments.of(FloatTag.valueOf(1.25F), 2.0D, "2.5f"),
                Arguments.of(DoubleTag.valueOf(-1.25D), 2.0D, "-2.5d")
        );
    }

}
