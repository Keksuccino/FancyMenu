package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslPassGraphTest {

    @Test
    void exposesEarlierBuffersFromTheCurrentFrameAndSelfOrLaterBuffersFromHistory() {
        boolean[] active = {true, true, true, true};
        GlslPassGraph.BufferVersion[][] expected = {
                {previous(), previous(), previous(), previous()},
                {current(), previous(), previous(), previous()},
                {current(), current(), previous(), previous()},
                {current(), current(), current(), previous()}
        };

        for (int renderingPass = 0; renderingPass < expected.length; renderingPass++) {
            for (int referencedBuffer = 0; referencedBuffer < expected[renderingPass].length; referencedBuffer++) {
                assertEquals(expected[renderingPass][referencedBuffer], GlslPassGraph.resolveBufferVersion(renderingPass, referencedBuffer, false, active), "Unexpected version for Buffer " + (char) ('A' + renderingPass) + " reading Buffer " + (char) ('A' + referencedBuffer));
            }
        }
    }

    @Test
    void exposesEveryActiveBufferCurrentFrameToTheImagePass() {
        boolean[] active = {true, true, true, true};

        assertAll(() -> assertEquals(current(), GlslPassGraph.resolveBufferVersion(4, 0, true, active)), () -> assertEquals(current(), GlslPassGraph.resolveBufferVersion(4, 1, true, active)), () -> assertEquals(current(), GlslPassGraph.resolveBufferVersion(4, 2, true, active)), () -> assertEquals(current(), GlslPassGraph.resolveBufferVersion(4, 3, true, active)));
    }

    @Test
    void routesInactiveAndOutOfRangeBufferReferencesToFallback() {
        boolean[] active = {true, false, true, false};

        assertAll(() -> assertEquals(GlslPassGraph.BufferVersion.FALLBACK, GlslPassGraph.resolveBufferVersion(3, 1, false, active)), () -> assertEquals(GlslPassGraph.BufferVersion.FALLBACK, GlslPassGraph.resolveBufferVersion(4, 3, true, active)), () -> assertEquals(GlslPassGraph.BufferVersion.FALLBACK, GlslPassGraph.resolveBufferVersion(0, -1, false, active)), () -> assertEquals(GlslPassGraph.BufferVersion.FALLBACK, GlslPassGraph.resolveBufferVersion(0, active.length, false, active)), () -> assertEquals(GlslPassGraph.BufferVersion.FALLBACK, GlslPassGraph.resolveBufferVersion(0, 3, false, new boolean[]{true})));
    }

    @Test
    void leavesTheCallerOwnedActivePassSnapshotUntouched() {
        boolean[] active = {true, false, true, true};
        boolean[] original = Arrays.copyOf(active, active.length);

        for (int renderingPass = 0; renderingPass < active.length; renderingPass++) {
            for (int referencedBuffer = 0; referencedBuffer < active.length; referencedBuffer++) {
                GlslPassGraph.resolveBufferVersion(renderingPass, referencedBuffer, false, active);
            }
        }

        assertArrayEquals(original, active);
    }

    private static GlslPassGraph.BufferVersion previous() {
        return GlslPassGraph.BufferVersion.PREVIOUS_FRAME;
    }

    private static GlslPassGraph.BufferVersion current() {
        return GlslPassGraph.BufferVersion.CURRENT_FRAME;
    }

}
