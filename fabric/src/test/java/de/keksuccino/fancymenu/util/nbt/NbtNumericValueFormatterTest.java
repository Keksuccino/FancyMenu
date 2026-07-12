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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NbtNumericValueFormatterTest {

    @ParameterizedTest
    @MethodSource("unitScaleValues")
    void unitScalePreservesExactSnbtForEveryNumericType(NumericTag tag, String expected) {
        assertEquals(expected, NbtNumericValueFormatter.format(tag, NbtNumericValueFormatter.DEFAULT_SCALE));
    }

    @ParameterizedTest
    @MethodSource("scaledValues")
    void nonUnitScaleRetainsTypeAndExistingConversionBehavior(NumericTag tag, double scale, String expected) {
        assertEquals(expected, NbtNumericValueFormatter.format(tag, scale));
    }

    @Test
    void integralScalingRoundsAndHandlesZeroAndNegativeValues() {
        assertEquals("2", NbtNumericValueFormatter.format(IntTag.valueOf(3), 0.5D));
        assertEquals("-1", NbtNumericValueFormatter.format(IntTag.valueOf(-3), 0.5D));
        assertEquals("0L", NbtNumericValueFormatter.format(LongTag.valueOf(123L), 0.0D));
        assertEquals("-6s", NbtNumericValueFormatter.format(ShortTag.valueOf((short) 3), -2.0D));
    }

    @Test
    void nonUnitOverflowPreservesExistingTypeAwareResults() {
        assertEquals("-1b", NbtNumericValueFormatter.format(ByteTag.valueOf((byte) 1), Double.MAX_VALUE));
        assertEquals("-1s", NbtNumericValueFormatter.format(ShortTag.valueOf((short) 1), Double.MAX_VALUE));
        assertEquals("-1", NbtNumericValueFormatter.format(IntTag.valueOf(1), Double.MAX_VALUE));
        assertEquals("9223372036854775807L", NbtNumericValueFormatter.format(LongTag.valueOf(1L), Double.MAX_VALUE));
        assertEquals("Infinityf", NbtNumericValueFormatter.format(FloatTag.valueOf(1.0F), Double.MAX_VALUE));
        assertEquals("Infinityd", NbtNumericValueFormatter.format(DoubleTag.valueOf(2.0D), Double.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource("invalidScaleStrings")
    void optionalScaleRejectsMissingMalformedAndNonFiniteValues(String scaleString) {
        assertNull(NbtNumericValueFormatter.parseOptionalScale(scaleString));
    }

    @ParameterizedTest
    @MethodSource("invalidScaleStrings")
    void defaultScaleFallsBackForMissingMalformedAndNonFiniteValues(String scaleString) {
        assertEquals(NbtNumericValueFormatter.DEFAULT_SCALE, NbtNumericValueFormatter.parseScale(scaleString));
    }

    @Test
    void parsersAcceptTrimmedExponentNotation() {
        assertEquals(Double.valueOf(250.0D), NbtNumericValueFormatter.parseOptionalScale(" 2.5e2 "));
        assertEquals(-0.4D, NbtNumericValueFormatter.parseScale("-4E-1"));
    }

    @ParameterizedTest
    @MethodSource("nonFiniteScales")
    void directNonFiniteScaleFallsBackToExactUnitScaleValue(double scale) {
        assertEquals("9223372036854775807L", NbtNumericValueFormatter.format(LongTag.valueOf(Long.MAX_VALUE), scale));
    }

    private static Stream<Arguments> unitScaleValues() {
        return Stream.of(Arguments.of(ByteTag.valueOf((byte) -12), "-12b"), Arguments.of(ShortTag.valueOf((short) -1234), "-1234s"), Arguments.of(IntTag.valueOf(-123456), "-123456"), Arguments.of(LongTag.valueOf(Long.MAX_VALUE), "9223372036854775807L"), Arguments.of(FloatTag.valueOf(1.25F), "1.25f"), Arguments.of(DoubleTag.valueOf(-2.5D), "-2.5d"));
    }

    private static Stream<Arguments> scaledValues() {
        return Stream.of(Arguments.of(ByteTag.valueOf((byte) 64), 2.0D, "-128b"), Arguments.of(ShortTag.valueOf((short) 20000), 2.0D, "-25536s"), Arguments.of(IntTag.valueOf(Integer.MAX_VALUE), 2.0D, "-2"), Arguments.of(LongTag.valueOf(Long.MAX_VALUE), 2.0D, "9223372036854775807L"), Arguments.of(FloatTag.valueOf(1.25F), 2.0D, "2.5f"), Arguments.of(DoubleTag.valueOf(1.25D), 2.0D, "2.5d"));
    }

    private static Stream<Arguments> invalidScaleStrings() {
        return Stream.of(Arguments.of((String) null), Arguments.of(""), Arguments.of(" \t "), Arguments.of("not-a-number"), Arguments.of("NaN"), Arguments.of("Infinity"), Arguments.of("-Infinity"), Arguments.of("1e309"));
    }

    private static Stream<Double> nonFiniteScales() {
        return Stream.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

}
