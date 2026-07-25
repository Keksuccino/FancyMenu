package de.keksuccino.fancymenu.util.rendering.glsl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslPassResourceLifecycleTest {

    @Test
    void releasesOnlyBufferPassesWhoseSourcesBecameInactive() {
        List<Integer> released = new ArrayList<>();

        GlslPassResourceLifecycle.releaseUnusedBufferPasses(true, new boolean[]{true, false, true, false}, released::add);

        assertEquals(List.of(1, 3), released);
    }

    @Test
    void keepsEveryActiveBufferPassWhileTheImageSourceRemainsAvailable() {
        List<Integer> released = new ArrayList<>();

        GlslPassResourceLifecycle.releaseUnusedBufferPasses(true, new boolean[]{true, true, true, true}, released::add);

        assertEquals(List.of(), released);
    }

    @Test
    void releasesEveryBufferPassWhenTheRequiredImageSourceIsAbsent() {
        List<Integer> released = new ArrayList<>();

        GlslPassResourceLifecycle.releaseUnusedBufferPasses(false, new boolean[]{true, false, true, false}, released::add);

        assertEquals(List.of(0, 1, 2, 3), released);
    }
}
