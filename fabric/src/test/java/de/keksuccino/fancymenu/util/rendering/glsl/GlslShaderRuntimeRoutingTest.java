package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslShaderRuntimeRoutingTest {

    @Test
    void routesBuiltInSamplerNamesToTheirChannelsAndCustomNamesToChannelZero() {
        assertAll(() -> assertEquals(0, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel0")), () -> assertEquals(1, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel1")), () -> assertEquals(2, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel2")), () -> assertEquals(3, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel3")), () -> assertEquals(0, GlslShaderRuntime.resolveSamplerChannelIndex("detailNoise")), () -> assertEquals(0, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel4")), () -> assertEquals(0, GlslShaderRuntime.resolveSamplerChannelIndex("ichannel1")), () -> assertEquals(0, GlslShaderRuntime.resolveSamplerChannelIndex("iChannel01")));
    }
}
