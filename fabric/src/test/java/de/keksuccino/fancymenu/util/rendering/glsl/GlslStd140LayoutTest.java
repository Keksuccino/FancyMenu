package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslStd140LayoutTest {

    @Test
    void alignsScalarsAndVectorsAndRoundsTheBlockSize() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("float", "scalar"), declaration("vec2", "pair"), declaration("vec3", "triple"), declaration("float", "trailing")));

        assertAll(() -> assertMember(layout, "scalar", 0, 4, 4), () -> assertMember(layout, "pair", 8, 8, 8), () -> assertMember(layout, "triple", 16, 12, 16), () -> assertMember(layout, "trailing", 28, 4, 4), () -> assertEquals(32, layout.size()));
    }

    @Test
    void usesSixteenByteArrayStrideForScalarAndVectorArrays() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(new GlslStd140Layout.Declaration("float", "scalars", 4), new GlslStd140Layout.Declaration("vec2", "pairs", 2), new GlslStd140Layout.Declaration("vec3", "triples", 2)));

        assertAll(() -> assertArrayMember(layout, "scalars", 0, 64, 4, 16), () -> assertArrayMember(layout, "pairs", 64, 32, 2, 16), () -> assertArrayMember(layout, "triples", 96, 32, 2, 16), () -> assertEquals(128, layout.size()));
    }

    @Test
    void usesOneColumnSlotPerMatrixColumnAndWholeMatrixArrayStride() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("mat2", "square"), declaration("mat3x2", "wide"), new GlslStd140Layout.Declaration("mat2", "matrices", 2)));

        GlslStd140Layout.Member square = layout.member("square");
        GlslStd140Layout.Member wide = layout.member("wide");
        GlslStd140Layout.Member matrices = layout.member("matrices");
        assertNotNull(square);
        assertNotNull(wide);
        assertNotNull(matrices);
        assertAll(() -> assertEquals(0, square.offset()), () -> assertEquals(32, square.size()), () -> assertEquals(2, square.columns()), () -> assertEquals(2, square.rows()), () -> assertEquals(32, wide.offset()), () -> assertEquals(48, wide.size()), () -> assertEquals(3, wide.columns()), () -> assertEquals(2, wide.rows()), () -> assertEquals(80, matrices.offset()), () -> assertEquals(64, matrices.size()), () -> assertEquals(32, matrices.arrayStride()), () -> assertEquals(144, layout.size()));
    }

    @Test
    void packsValuesAtTheirDeclaredOffsetsAndLeavesPaddingZeroed() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("float", "scalar"), declaration("vec3", "vector"), new GlslStd140Layout.Declaration("float", "samples", 3), declaration("ivec2", "indices"), declaration("bool", "enabled")));

        ByteBuffer buffer = layout.pack(Map.of("scalar", 1.5F, "vector", new float[]{2.0F, 3.0F, 4.0F}, "samples", new float[]{10.0F, 20.0F, 30.0F}, "indices", new int[]{7, 9}, "enabled", true));

        assertAll(() -> assertTrue(buffer.isDirect()), () -> assertEquals(ByteOrder.nativeOrder(), buffer.order()), () -> assertEquals(0, buffer.position()), () -> assertEquals(layout.size(), buffer.limit()), () -> assertEquals(1.5F, buffer.getFloat(0)), () -> assertZeroRange(buffer, 4, 16), () -> assertEquals(2.0F, buffer.getFloat(16)), () -> assertEquals(3.0F, buffer.getFloat(20)), () -> assertEquals(4.0F, buffer.getFloat(24)), () -> assertEquals(0, buffer.getInt(28)), () -> assertEquals(10.0F, buffer.getFloat(32)), () -> assertZeroRange(buffer, 36, 48), () -> assertEquals(20.0F, buffer.getFloat(48)), () -> assertZeroRange(buffer, 52, 64), () -> assertEquals(30.0F, buffer.getFloat(64)), () -> assertZeroRange(buffer, 68, 80), () -> assertEquals(7, buffer.getInt(80)), () -> assertEquals(9, buffer.getInt(84)), () -> assertEquals(1, buffer.getInt(88)), () -> assertZeroRange(buffer, 92, layout.size()));
    }

    @Test
    void packsMatrixColumnsWithSixteenByteColumnStride() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("mat2", "matrix")));

        ByteBuffer buffer = layout.pack(Map.of("matrix", new float[]{1.0F, 2.0F, 3.0F, 4.0F}));

        assertAll(() -> assertEquals(1.0F, buffer.getFloat(0)), () -> assertEquals(2.0F, buffer.getFloat(4)), () -> assertZeroRange(buffer, 8, 16), () -> assertEquals(3.0F, buffer.getFloat(16)), () -> assertEquals(4.0F, buffer.getFloat(20)), () -> assertZeroRange(buffer, 24, 32));
    }

    @Test
    void distinguishesScalarFromSingleElementArray() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("float", "scalar"), new GlslStd140Layout.Declaration("float", "array", 1)));

        assertAll(() -> assertMember(layout, "scalar", 0, 4, 4), () -> assertArrayMember(layout, "array", 16, 16, 1, 16), () -> assertEquals(32, layout.size()));
    }

    @Test
    void representsAllIntegerCompatibleKindsAsFourByteComponents() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("int", "signed"), declaration("uint", "unsigned"), declaration("bool", "flag"), declaration("bvec2", "flags"), declaration("uvec3", "wideUnsigned")));
        ByteBuffer buffer = layout.pack(Map.of("signed", -3, "unsigned", 4, "flag", true, "flags", new int[]{1, 0}, "wideUnsigned", new int[]{5, 6, 7}));

        assertAll(() -> assertEquals(GlslStd140Layout.ScalarKind.SIGNED_INT, layout.member("signed").scalarKind()), () -> assertEquals(GlslStd140Layout.ScalarKind.UNSIGNED_INT, layout.member("unsigned").scalarKind()), () -> assertEquals(GlslStd140Layout.ScalarKind.BOOLEAN, layout.member("flag").scalarKind()), () -> assertEquals(-3, buffer.getInt(0)), () -> assertEquals(4, buffer.getInt(4)), () -> assertEquals(1, buffer.getInt(8)), () -> assertEquals(1, buffer.getInt(16)), () -> assertEquals(0, buffer.getInt(20)), () -> assertEquals(5, buffer.getInt(32)), () -> assertEquals(6, buffer.getInt(36)), () -> assertEquals(7, buffer.getInt(40)));
    }

    @Test
    void leavesMissingAndPartiallyProvidedValuesZeroInitialized() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("vec4", "partial"), declaration("ivec4", "missing")));

        ByteBuffer buffer = layout.pack(Map.of("partial", new float[]{8.0F, 9.0F}));

        assertAll(() -> assertEquals(8.0F, buffer.getFloat(0)), () -> assertEquals(9.0F, buffer.getFloat(4)), () -> assertZeroRange(buffer, 8, layout.size()));
    }

    @Test
    void rejectsDuplicateAndUnsupportedDeclarations() {
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () -> GlslStd140Layout.create(List.of(declaration("float", "same"), declaration("int", "same"))));
        IllegalArgumentException unsupported = assertThrows(IllegalArgumentException.class, () -> GlslStd140Layout.create(List.of(declaration("sampler2D", "texture"))));

        assertAll(() -> assertTrue(duplicate.getMessage().contains("same")), () -> assertTrue(unsupported.getMessage().contains("sampler2D")));
    }

    @Test
    void acceptsTheExactCrossBackendUniformBlockLimit() {
        int scalarCount = GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE / 16;

        GlslStd140Layout layout = GlslStd140Layout.create(List.of(new GlslStd140Layout.Declaration("float", "values", scalarCount)));

        assertAll(() -> assertEquals(16_384, GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE), () -> assertEquals(GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE, layout.size()), () -> assertEquals(GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE, layout.createBuffer().capacity()));
    }

    @Test
    void rejectsOversizedCumulativeAndPathologicalUniformBlocks() {
        int exactScalarCount = GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE / 16;
        IllegalArgumentException oneStrideOver = assertThrows(IllegalArgumentException.class, () -> GlslStd140Layout.create(List.of(new GlslStd140Layout.Declaration("float", "oneStrideOver", exactScalarCount + 1))));
        IllegalArgumentException cumulative = assertThrows(IllegalArgumentException.class, () -> GlslStd140Layout.create(List.of(new GlslStd140Layout.Declaration("float", "values", exactScalarCount - 1), declaration("vec4", "lastSlot"), declaration("float", "cumulativeOverflow"))));
        IllegalArgumentException pathological = assertThrows(IllegalArgumentException.class, () -> GlslStd140Layout.create(List.of(new GlslStd140Layout.Declaration("mat4", "pathological", Integer.MAX_VALUE))));

        assertAll(() -> assertLimitDiagnostic(oneStrideOver, "oneStrideOver"), () -> assertLimitDiagnostic(cumulative, "cumulativeOverflow"), () -> assertLimitDiagnostic(pathological, "pathological"));
    }

    @Test
    void rejectsUnknownTypeMismatchedWritesAndExcessComponents() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("float", "floating"), declaration("ivec2", "integers")));

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> layout.writeFloats(layout.createBuffer(), "missing", 1.0F));
        IllegalArgumentException floatToInt = assertThrows(IllegalArgumentException.class, () -> layout.writeFloats(layout.createBuffer(), "integers", 1.0F));
        IllegalArgumentException intToFloat = assertThrows(IllegalArgumentException.class, () -> layout.writeInts(layout.createBuffer(), "floating", 1));
        IllegalArgumentException excess = assertThrows(IllegalArgumentException.class, () -> layout.writeInts(layout.createBuffer(), "integers", 1, 2, 3));
        IllegalArgumentException unsupportedValue = assertThrows(IllegalArgumentException.class, () -> layout.pack(Map.of("floating", "one")));

        assertAll(() -> assertTrue(unknown.getMessage().contains("missing")), () -> assertTrue(floatToInt.getMessage().contains("integers")), () -> assertTrue(intToFloat.getMessage().contains("floating")), () -> assertTrue(excess.getMessage().contains("expected at most 2")), () -> assertTrue(unsupportedValue.getMessage().contains(String.class.getName())));
    }

    @Test
    void validatesDeclarationBoundariesAndFormatsShaderMembers() {
        IllegalArgumentException blankType = assertThrows(IllegalArgumentException.class, () -> new GlslStd140Layout.Declaration(" ", "value", 0));
        IllegalArgumentException blankName = assertThrows(IllegalArgumentException.class, () -> new GlslStd140Layout.Declaration("float", " ", 0));
        IllegalArgumentException negativeArray = assertThrows(IllegalArgumentException.class, () -> new GlslStd140Layout.Declaration("float", "values", -1));

        assertAll(() -> assertEquals("vec3 value;", declaration("vec3", "value").toShaderDeclaration()), () -> assertEquals("float values[4];", new GlslStd140Layout.Declaration("float", "values", 4).toShaderDeclaration()), () -> assertTrue(blankType.getMessage().contains("type")), () -> assertTrue(blankName.getMessage().contains("name")), () -> assertTrue(negativeArray.getMessage().contains("negative")));
    }

    @Test
    void exposesAnImmutableOrderedMemberViewAndHandlesEmptyLayouts() {
        GlslStd140Layout layout = GlslStd140Layout.create(List.of(declaration("float", "first"), declaration("int", "second")));
        GlslStd140Layout empty = GlslStd140Layout.create(List.of());

        assertAll(() -> assertEquals(List.of("first", "second"), layout.members().stream().map(GlslStd140Layout.Member::name).toList()), () -> assertThrows(UnsupportedOperationException.class, () -> layout.members().clear()), () -> assertNull(layout.member("missing")), () -> assertEquals(0, empty.size()), () -> assertEquals(0, empty.createBuffer().capacity()), () -> assertFalse(empty.members().iterator().hasNext()));
    }

    private static GlslStd140Layout.Declaration declaration(String type, String name) {
        return new GlslStd140Layout.Declaration(type, name, 0);
    }

    private static void assertMember(GlslStd140Layout layout, String name, int offset, int size, int alignment) {
        GlslStd140Layout.Member member = layout.member(name);
        assertNotNull(member);
        assertAll(() -> assertEquals(offset, member.offset()), () -> assertEquals(size, member.size()), () -> assertEquals(alignment, member.alignment()), () -> assertEquals(0, member.arrayLength()), () -> assertEquals(0, member.arrayStride()), () -> assertEquals(1, member.elementCount()));
    }

    private static void assertArrayMember(GlslStd140Layout layout, String name, int offset, int size, int arrayLength, int arrayStride) {
        GlslStd140Layout.Member member = layout.member(name);
        assertNotNull(member);
        assertAll(() -> assertEquals(offset, member.offset()), () -> assertEquals(size, member.size()), () -> assertEquals(16, member.alignment()), () -> assertEquals(arrayLength, member.arrayLength()), () -> assertEquals(arrayStride, member.arrayStride()), () -> assertEquals(arrayLength, member.elementCount()));
    }

    private static void assertLimitDiagnostic(IllegalArgumentException exception, String declarationName) {
        assertAll(() -> assertTrue(exception.getMessage().contains(declarationName)), () -> assertTrue(exception.getMessage().contains(Integer.toString(GlslStd140Layout.MAX_UNIFORM_BLOCK_SIZE))), () -> assertTrue(exception.getMessage().contains("cross-backend limit")));
    }

    private static void assertZeroRange(ByteBuffer buffer, int startInclusive, int endExclusive) {
        for (int offset = startInclusive; offset < endExclusive; offset++) {
            assertEquals(0, buffer.get(offset), "Expected std140 padding byte at offset " + offset + " to remain zero");
        }
    }

}
