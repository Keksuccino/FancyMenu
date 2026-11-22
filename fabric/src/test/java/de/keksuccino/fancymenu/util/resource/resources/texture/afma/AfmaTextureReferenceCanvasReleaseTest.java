package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.LogCapture;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reference canvas of block-inter frames belongs to the render thread, which applies frames onto it without a lock. Checks with real stream threads that only the
 * render thread closes it: a stream thread that fails or that releases the decoder of a closed texture never closes it, because the render thread can still use it then,
 * and a second close of another thread at the same time would free it twice.
 */
class AfmaTextureReferenceCanvasReleaseTest {

    private final NativeImage streamingCanvas = new NativeImage(2, 2, true);

    @AfterEach
    void closeStreamingCanvas() {
        this.streamingCanvas.close();
    }

    @Test
    void aFailingStreamThreadKeepsTheCanvasTheRenderThreadAppliesAFrameOnto() throws InterruptedException {
        FailingStreamTexture texture = new FailingStreamTexture();
        // What applyPreparedFrame() does on the render thread for a block-inter frame, before it copies the tiles of the frame from the canvas
        NativeImage referenceCanvas = texture.ensureBlockInterReferenceCanvas(this.streamingCanvas);

        try (LogCapture ignored = LogCapture.problemsOf(AfmaTexture.class)) {
            texture.startTickerIfNeeded();
            Thread stream = texture.streamThread;
            assertNotNull(stream);
            ReadGate.join(stream);
        }
        assertTrue(texture.isLoadingFailed(), "The stream thread didn't fail");
        assertTrue(isAllocated(referenceCanvas), "The failing stream thread closed the reference canvas while the render thread applied a frame onto it");
        assertSame(referenceCanvas, texture.blockInterReferenceCanvas);

        texture.close();
        assertFalse(isAllocated(referenceCanvas), "Closing the texture didn't close the reference canvas");
    }

    @Test
    void closingClosesTheCanvasRightAwayWhileTheStreamThreadStillReads() throws InterruptedException {
        ReadGate read = new ReadGate();
        ReadingStreamTexture texture = new ReadingStreamTexture(read);
        NativeImage referenceCanvas = texture.ensureBlockInterReferenceCanvas(this.streamingCanvas);
        texture.startTickerIfNeeded();
        Thread stream = texture.streamThread;
        assertNotNull(stream);

        try {
            read.awaitReached();
            texture.close();
            // The stream thread still reads, so it releases the decoder once it stops, but the canvas got closed by the closing thread
            assertNotNull(texture.decoder);
            assertFalse(isAllocated(referenceCanvas), "Closing the texture left closing the reference canvas to the stream thread");
            assertNull(texture.blockInterReferenceCanvas);
        } finally {
            read.open();
        }

        ReadGate.join(stream);
        assertNull(texture.decoder, "The stream thread didn't release the decoder");
    }

    /**
     * If the reference canvas still holds its memory, checked by copying it onto the streaming canvas, like block-inter frames copy their tiles from it.
     * A closed image throws before its memory gets touched.
     */
    private boolean isAllocated(@NotNull NativeImage referenceCanvas) {
        try {
            this.streamingCanvas.copyFrom(referenceCanvas);
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    /**
     * A decoded texture whose real stream thread reads the payload of its full frame from a decoder without an open archive, so the read fails like an I/O error does.
     */
    private static final class FailingStreamTexture extends AfmaTexture {

        private FailingStreamTexture() {
            this.decoder = new AfmaDecoder();
            this.frameIndex = new AfmaFrameIndex(List.of(AfmaFrameDescriptor.full("frame")), List.of());
            this.frameCount = 1;
            this.frameDelaysMs = new long[]{100L};
            this.decoded.set(true);
        }

    }

    /**
     * A decoded texture whose real stream thread waits at a gate while it reads its first frame, like a slow disk.
     */
    private static final class ReadingStreamTexture extends AfmaTexture {

        @NotNull
        private final ReadGate read;

        private ReadingStreamTexture(@NotNull ReadGate read) {
            this.read = read;
            this.decoder = new AfmaDecoder();
            this.frameCount = 1;
            this.frameDelaysMs = new long[]{100L};
            this.decoded.set(true);
        }

        @Override
        @Nullable
        protected PreparedFrame prepareNextFrame(int generation) {
            this.read.pass();
            return null;
        }

    }

}
