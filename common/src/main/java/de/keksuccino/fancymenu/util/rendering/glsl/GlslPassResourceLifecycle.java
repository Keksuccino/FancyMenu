package de.keksuccino.fancymenu.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;

/** Pure pass-lifecycle policy that keeps GPU resource ownership decisions testable without a rendering device. */
final class GlslPassResourceLifecycle {

    private GlslPassResourceLifecycle() {
    }

    /**
     * Releases inactive buffer passes and, because no buffer can execute without the required Image pass, every buffer
     * pass when the Image source is absent. The callback owns both the pass program and its feedback target cleanup.
     */
    static void releaseUnusedBufferPasses(boolean imageSourceActive, boolean @NotNull [] activeBufferPasses, @NotNull IntConsumer releasePass) {
        for (int passIndex = 0; passIndex < activeBufferPasses.length; passIndex++) {
            if (!imageSourceActive || !activeBufferPasses[passIndex]) {
                releasePass.accept(passIndex);
            }
        }
    }
}
