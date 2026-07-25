package de.keksuccino.fancymenu.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;

/** Pure description of the A-to-D immediate-swap feedback ordering used by the GPU runtime. */
public final class GlslPassGraph {

    private GlslPassGraph() {
    }

    @NotNull
    public static BufferVersion resolveBufferVersion(int renderingPassIndex, int referencedBufferIndex, boolean imagePass, @NotNull boolean[] activeBufferPasses) {
        if (referencedBufferIndex < 0 || referencedBufferIndex >= activeBufferPasses.length || !activeBufferPasses[referencedBufferIndex]) {
            return BufferVersion.FALLBACK;
        }
        if (imagePass || referencedBufferIndex < renderingPassIndex) {
            return BufferVersion.CURRENT_FRAME;
        }
        return BufferVersion.PREVIOUS_FRAME;
    }

    public enum BufferVersion {
        PREVIOUS_FRAME,
        CURRENT_FRAME,
        FALLBACK
    }

}
