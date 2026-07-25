package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslFrameUniformStateTest {

    @Test
    void writesOneFrameInputSnapshotConsistentlyAcrossBufferAndImagePasses() {
        GlslStd140Layout layout = uniformLayout();
        GlslFrameUniformState.FrameSnapshot snapshot = frameSnapshot(Map.of(), 0);
        GlslFrameUniformState.ChannelResolution[] channels = channels();
        ByteBuffer bufferPass = layout.createBuffer();
        ByteBuffer imagePass = layout.createBuffer();

        snapshot.write(layout, bufferPass, bufferContext(), channels);
        snapshot.write(layout, imagePass, imageContext(), channels);

        List<String> frameScopedMembers = List.of("iResolution", "iTime", "iTimeDelta", "iFrameRate", "iMouse", "iDate", "iSampleRate", "iChannelTime", "iChannelResolution", "fmAreaSize", "fmScreenSize", "fmGuiScale", "fmMouse", "fmMouseDelta", "fmMouseButtons", "fmMouseClickCount", "fmMouseReleaseCount", "fmMouseScroll", "fmMouseScrollTotal", "fmKeyEvent", "fmKeyEventCount", "fmCharEvent", "fmCharEventCount", "fmDateParts", "fmTimeParts", "fmDayOfYear", "fmWeekOfYear", "fmUnixTimeSeconds", "fmUnixTimeMilliseconds", "fmPartialTick", "fmGameDeltaTicks", "fmRealtimeDeltaTicks", "fmInWorld", "fmIsPaused", "fmOpacity", "fmVariableCount");
        for (String member : frameScopedMembers) {
            assertMemberBytesEqual(layout, member, bufferPass, imagePass);
        }
        assertAll(() -> assertIntVector(layout, bufferPass, "iFrame", 7), () -> assertIntVector(layout, imagePass, "iFrame", 42), () -> assertFloatVector(layout, bufferPass, "fmMouseDelta", 5.0F, -6.0F), () -> assertFloatVector(layout, imagePass, "fmMouseDelta", 5.0F, -6.0F), () -> assertFloatVector(layout, bufferPass, "fmMouseScroll", 1.25F, -2.5F), () -> assertFloatVector(layout, imagePass, "fmMouseScroll", 1.25F, -2.5F), () -> assertIntVector(layout, bufferPass, "fmKeyEvent", 65, 30, 2, 1), () -> assertIntVector(layout, imagePass, "fmKeyEvent", 65, 30, 2, 1));
    }

    @Test
    void separatesClippedInternalRenderAreaFromLegacyFullAreaUniforms() {
        GlslStd140Layout layout = uniformLayout();
        GlslFrameUniformState.FrameSnapshot snapshot = frameSnapshot(Map.of(), 0);
        ByteBuffer bufferPass = layout.createBuffer();
        ByteBuffer imagePass = layout.createBuffer();

        snapshot.write(layout, bufferPass, bufferContext(), channels());
        snapshot.write(layout, imagePass, imageContext(), channels());

        assertAll(() -> assertFloatVector(layout, bufferPass, GlslShaderSourceTransformer.RENDER_AREA_UNIFORM, 0.0F, 0.0F, 640.0F, 360.0F), () -> assertFloatVector(layout, imagePass, GlslShaderSourceTransformer.RENDER_AREA_UNIFORM, 120.0F, 540.0F, 600.0F, 320.0F), () -> assertFloatVector(layout, bufferPass, GlslShaderSourceTransformer.RENDER_TARGET_SIZE_UNIFORM, 640.0F, 360.0F), () -> assertFloatVector(layout, imagePass, GlslShaderSourceTransformer.RENDER_TARGET_SIZE_UNIFORM, 1920.0F, 1080.0F), () -> assertFloatVector(layout, bufferPass, "iResolution", 640.0F, 360.0F, 1.0F), () -> assertFloatVector(layout, imagePass, "iResolution", 640.0F, 360.0F, 1.0F), () -> assertFloatVector(layout, bufferPass, "fmAreaOffset", 0.0F, 0.0F), () -> assertFloatVector(layout, imagePass, "fmAreaOffset", 100.0F, 520.0F), () -> assertFloatVector(layout, imagePass, "fmAreaTopLeft", 100.0F, 200.0F), () -> assertFloatVector(layout, imagePass, "fmAreaSize", 640.0F, 360.0F));
    }

    @Test
    void writesStd140ChannelResolutionArraysWithMissingChannelsNeutralized() {
        GlslStd140Layout layout = uniformLayout();
        ByteBuffer buffer = layout.createBuffer();

        frameSnapshot(Map.of(), 0).write(layout, buffer, bufferContext(), channels());

        GlslStd140Layout.Member member = layout.member("iChannelResolution");
        assertAll(() -> assertArrayVec3(buffer, member, 0, 64.0F, 32.0F, 1.0F), () -> assertArrayVec3(buffer, member, 1, 0.0F, 0.0F, 0.0F), () -> assertArrayVec3(buffer, member, 2, 512.0F, 256.0F, 1.0F), () -> assertArrayVec3(buffer, member, 3, 1.0F, 0.0F, 0.0F));
    }

    @Test
    void writesAllDynamicVariableFamiliesAndKeepsTheOriginalVariableCount() {
        GlslStd140Layout layout = uniformLayout();
        GlslFrameUniformState.VariableValue value = new GlslFrameUniformState.VariableValue(1.5F, 2, 1, new float[]{3.0F, 4.0F, 5.0F, 6.0F});
        ByteBuffer buffer = layout.createBuffer();

        frameSnapshot(Map.of("score", value), 9).write(layout, buffer, bufferContext(), channels());

        assertAll(() -> assertFloatVector(layout, buffer, "fmVarFloat_score", 1.5F), () -> assertIntVector(layout, buffer, "fmVarInt_score", 2), () -> assertIntVector(layout, buffer, "fmVarBool_score", 1), () -> assertFloatVector(layout, buffer, "fmVarVec2_score", 3.0F, 4.0F), () -> assertFloatVector(layout, buffer, "fmVarVec3_score", 3.0F, 4.0F, 5.0F), () -> assertFloatVector(layout, buffer, "fmVarVec4_score", 3.0F, 4.0F, 5.0F, 6.0F), () -> assertIntVector(layout, buffer, "fmVarExists_score", 1), () -> assertIntVector(layout, buffer, "fmVariableCount", 9));
    }

    @Test
    void repeatedPassWritesDoNotConsumeOrMutateFrameDeltas() {
        GlslStd140Layout layout = uniformLayout();
        GlslFrameUniformState.FrameSnapshot snapshot = frameSnapshot(Map.of(), 0);
        ByteBuffer first = layout.createBuffer();
        ByteBuffer second = layout.createBuffer();

        snapshot.write(layout, first, bufferContext(), channels());
        snapshot.write(layout, second, bufferContext(), channels());

        assertAll(() -> assertEquals(first, second), () -> assertFloatVector(layout, first, "fmMouseDelta", 5.0F, -6.0F), () -> assertFloatVector(layout, second, "fmMouseDelta", 5.0F, -6.0F), () -> assertFloatVector(layout, first, "fmMouseScroll", 1.25F, -2.5F), () -> assertFloatVector(layout, second, "fmMouseScroll", 1.25F, -2.5F));
    }

    private static GlslStd140Layout uniformLayout() {
        String source = """
                uniform float fmVarFloat_score;
                uniform int fmVarInt_score;
                uniform bool fmVarBool_score;
                uniform vec2 fmVarVec2_score;
                uniform vec3 fmVarVec3_score;
                uniform vec4 fmVarVec4_score;
                uniform int fmVarExists_score;
                void main() {
                    gl_FragColor = vec4(fmVarFloat_score + float(fmVarInt_score + fmVarBool_score + fmVarExists_score)) + fmVarVec4_score + vec4(fmVarVec2_score, fmVarVec3_score.x, 0.0);
                }
                """;
        return GlslShaderSourceTransformer.buildFragmentVariants(source, GlslShaderRuntime.CompileMode.DIRECT, false, GlslShaderSourceTransformer.BackendCoordinates.VULKAN, GlslShaderSourceTransformer.PassKind.IMAGE).getFirst().uniformLayout();
    }

    private static GlslFrameUniformState.FrameSnapshot frameSnapshot(Map<String, GlslFrameUniformState.VariableValue> variables, int originalVariableCount) {
        return new GlslFrameUniformState.FrameSnapshot(12.5F, 0.25F, 60.0F, new float[]{11.0F, 12.0F, 13.0F, 14.0F}, new float[]{2026.0F, 7.0F, 25.0F, 12_345.5F}, 2.0F, new float[]{21.0F, 22.0F, 0.25F, 0.75F}, new float[]{5.0F, -6.0F}, new int[]{1, 0, 1, 0}, new int[]{2, 3, 4, 5}, new int[]{6, 7, 8, 9}, new float[]{1.25F, -2.5F}, new float[]{10.0F, 20.0F}, new int[]{65, 30, 2, 1}, 8, new int[]{0x1F680, 4, 0, 0}, 9, new int[]{2026, 7, 25, 6}, new int[]{14, 30, 45, 321}, 206, 30, 1_234_567_890, 321, 0.5F, 1.25F, 0.75F, 1, 0, 0.8F, 1920, 1080, 640, 360, variables, originalVariableCount);
    }

    private static GlslFrameUniformState.PassContext bufferContext() {
        return new GlslFrameUniformState.PassContext(0, 0, 0, 0, 0, 640, 360, 640, 360, 7);
    }

    private static GlslFrameUniformState.PassContext imageContext() {
        return new GlslFrameUniformState.PassContext(100, 200, 520, 120, 540, 600, 320, 1920, 1080, 42);
    }

    private static GlslFrameUniformState.ChannelResolution[] channels() {
        return new GlslFrameUniformState.ChannelResolution[]{new GlslFrameUniformState.ChannelResolution(64.0F, 32.0F), new GlslFrameUniformState.ChannelResolution(0.0F, 0.0F), new GlslFrameUniformState.ChannelResolution(512.0F, 256.0F), new GlslFrameUniformState.ChannelResolution(1.0F, 0.0F)};
    }

    private static void assertMemberBytesEqual(GlslStd140Layout layout, String name, ByteBuffer first, ByteBuffer second) {
        GlslStd140Layout.Member member = layout.member(name);
        for (int offset = member.offset(); offset < member.offset() + member.size(); offset++) {
            assertEquals(first.get(offset), second.get(offset), "Frame-scoped uniform '" + name + "' differed at byte " + offset);
        }
    }

    private static void assertFloatVector(GlslStd140Layout layout, ByteBuffer buffer, String name, float... expected) {
        GlslStd140Layout.Member member = layout.member(name);
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], buffer.getFloat(member.offset() + index * Float.BYTES), "Unexpected component " + index + " for " + name);
        }
    }

    private static void assertIntVector(GlslStd140Layout layout, ByteBuffer buffer, String name, int... expected) {
        GlslStd140Layout.Member member = layout.member(name);
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], buffer.getInt(member.offset() + index * Integer.BYTES), "Unexpected component " + index + " for " + name);
        }
    }

    private static void assertArrayVec3(ByteBuffer buffer, GlslStd140Layout.Member member, int index, float x, float y, float z) {
        int offset = member.offset() + index * member.arrayStride();
        assertAll(() -> assertEquals(x, buffer.getFloat(offset)), () -> assertEquals(y, buffer.getFloat(offset + Float.BYTES)), () -> assertEquals(z, buffer.getFloat(offset + Float.BYTES * 2)));
    }

}
