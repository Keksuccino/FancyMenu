package de.keksuccino.fancymenu.customization.global;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Keeps the latest frame of the world the player is in, so it can get saved as seamless world loading screenshot when the player leaves.
 * <p>
 * Reading a frame back from the GPU into CPU memory takes long enough to make the game stutter, because the render thread has to wait for
 * the GPU and convert every pixel. So while the player is in the world, frames only get copied on the GPU, which is cheap and does not block
 * the render thread. Only the latest copy gets read back, once, when the player leaves the world.
 * <p>
 * Only use this on the render thread.
 */
final class SeamlessWorldFrameCapture {

    private static final Logger LOGGER = LogManager.getLogger();
    static final long COPY_INTERVAL_MS = 1000L;
    private static final long PENDING_READ_BACK_POLL_INTERVAL_NS = 1_000_000L;

    @NotNull private final Supplier<FrameCopy> frameCopyFactory;
    @NotNull private final LongSupplier clockMs;
    @NotNull private final Runnable pendingGpuTaskRunner;
    @Nullable private FrameCopy frameCopy;
    private boolean hasFrame;
    private long nextCopyTimeMs = Long.MIN_VALUE;
    private int pendingReadBacks;

    /**
     * @param frameCopyFactory creates the GPU copy a capture copies its frames into
     * @param clockMs a monotonic clock in milliseconds
     * @param pendingGpuTaskRunner runs the callbacks of GPU work that finished, which includes the callbacks of read backs
     */
    SeamlessWorldFrameCapture(@NotNull Supplier<FrameCopy> frameCopyFactory, @NotNull LongSupplier clockMs, @NotNull Runnable pendingGpuTaskRunner) {
        this.frameCopyFactory = frameCopyFactory;
        this.clockMs = clockMs;
        this.pendingGpuTaskRunner = pendingGpuTaskRunner;
    }

    @NotNull
    static SeamlessWorldFrameCapture create() {
        return new SeamlessWorldFrameCapture(GpuFrameCopy::new, Util::getMillis, RenderSystem::executePendingTasks);
    }

    /**
     * Copies the color of the given render target on the GPU, at most once per {@link #COPY_INTERVAL_MS}.
     */
    void copyFrameIfDue(@NotNull RenderTarget source) {
        long now = this.clockMs.getAsLong();
        if (now < this.nextCopyTimeMs) return;
        // Set before copying, so a copy that keeps failing only gets retried once per interval instead of every frame.
        this.nextCopyTimeMs = now + COPY_INTERVAL_MS;
        if (this.frameCopy == null) this.frameCopy = this.frameCopyFactory.get();
        // A copy that fails halfway can leave the previous frame incomplete, so only a finished copy counts as frame.
        this.hasFrame = false;
        this.hasFrame = this.frameCopy.copyFrom(source);
    }

    /**
     * Starts reading the latest frame back into CPU memory and resets the capture, so the next world starts without a frame.<br>
     * The consumer gets called exactly once, with {@code null} if reading the frame back failed. It can get called later on the render thread,
     * once the GPU finished reading the frame back. The consumer owns the image and has to close it.
     *
     * @return {@code false} if there was no frame to read back, in which case the consumer never gets called.
     */
    boolean readBackAndReset(@NotNull Consumer<NativeImage> imageConsumer) {
        FrameCopy copy = this.frameCopy;
        boolean copyHasFrame = this.hasFrame;
        this.frameCopy = null;
        this.reset();
        if (copy == null) return false;
        if (!copyHasFrame) {
            releaseSafely(copy);
            return false;
        }
        PendingReadBack readBack = new PendingReadBack(imageConsumer);
        this.pendingReadBacks++;
        try {
            copy.readBack(readBack::complete);
        } catch (RuntimeException ex) {
            LOGGER.warn("[FANCYMENU] Failed to read back seamless world loading screenshot.", ex);
            readBack.complete(null);
        }
        return true;
    }

    /**
     * Releases the latest frame. Frames that are getting read back are not affected.
     */
    void reset() {
        FrameCopy copy = this.frameCopy;
        this.frameCopy = null;
        this.hasFrame = false;
        this.nextCopyTimeMs = Long.MIN_VALUE;
        if (copy != null) releaseSafely(copy);
    }

    boolean hasPendingReadBacks() {
        return this.pendingReadBacks > 0;
    }

    /**
     * Waits until all frames that are getting read back are finished, so they still get saved when the game closes right after leaving a world.
     * Gives up after the given timeout.
     */
    void finishPendingReadBacks(long timeoutMs) {
        long deadline = this.clockMs.getAsLong() + timeoutMs;
        while (this.hasPendingReadBacks()) {
            this.pendingGpuTaskRunner.run();
            if (!this.hasPendingReadBacks()) return;
            if (this.clockMs.getAsLong() >= deadline) {
                LOGGER.warn("[FANCYMENU] Gave up waiting for {} seamless world loading screenshot(s) to finish reading back from the GPU.", this.pendingReadBacks);
                return;
            }
            LockSupport.parkNanos(PENDING_READ_BACK_POLL_INTERVAL_NS);
        }
    }

    private static void releaseSafely(@NotNull FrameCopy copy) {
        try {
            copy.release();
        } catch (RuntimeException ex) {
            LOGGER.warn("[FANCYMENU] Failed to release seamless world loading frame copy.", ex);
        }
    }

    /**
     * A copy of a frame on the GPU.
     */
    interface FrameCopy {

        /**
         * Copies the color of the given render target into this copy.
         *
         * @return {@code false} if the render target had nothing to copy.
         */
        boolean copyFrom(@NotNull RenderTarget source);

        /**
         * Reads this copy back into CPU memory and releases it afterwards.<br>
         * Has to call the consumer exactly once, right away or later on the render thread, with {@code null} if the copy had no frame.
         * Throws if reading the copy back failed.
         */
        void readBack(@NotNull Consumer<NativeImage> imageConsumer);

        /**
         * Releases this copy without reading it back.
         */
        void release();

    }

    /**
     * Hands the result of a read back to its consumer exactly once and keeps track of the read backs that are not finished yet.
     */
    private final class PendingReadBack {

        @NotNull private final Consumer<NativeImage> imageConsumer;
        private boolean completed;

        private PendingReadBack(@NotNull Consumer<NativeImage> imageConsumer) {
            this.imageConsumer = imageConsumer;
        }

        private void complete(@Nullable NativeImage image) {
            if (this.completed) {
                // The read back already got completed as failed, so this late image belongs to nobody.
                if (image != null) image.close();
                return;
            }
            this.completed = true;
            SeamlessWorldFrameCapture.this.pendingReadBacks--;
            try {
                this.imageConsumer.accept(image);
            } catch (RuntimeException ex) {
                LOGGER.warn("[FANCYMENU] Failed to handle seamless world loading screenshot.", ex);
            }
        }

    }

    /**
     * Copies frames into a texture with the size and format of the copied render target.
     */
    private static final class GpuFrameCopy implements FrameCopy {

        @Nullable private TextureTarget target;

        @Override
        public boolean copyFrom(@NotNull RenderTarget source) {
            GpuTexture sourceTexture = source.getColorTexture();
            if (sourceTexture == null) return false;
            TextureTarget copyTarget = this.target;
            GpuTexture copyTexture = (copyTarget != null) ? copyTarget.getColorTexture() : null;
            if ((copyTarget != null) && ((copyTexture == null) || (copyTexture.getFormat() != sourceTexture.getFormat()))) {
                copyTarget.destroyBuffers();
                copyTarget = null;
            }
            if (copyTarget == null) {
                copyTarget = new TextureTarget("FancyMenu Seamless World Loading Frame", source.width, source.height, false, sourceTexture.getFormat());
                this.target = copyTarget;
            } else if ((copyTarget.width != source.width) || (copyTarget.height != source.height)) {
                copyTarget.resize(source.width, source.height);
            }
            copyTexture = copyTarget.getColorTexture();
            if (copyTexture == null) return false;
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(sourceTexture, copyTexture, 0, 0, 0, 0, 0, source.width, source.height);
            return true;
        }

        @Override
        public void readBack(@NotNull Consumer<NativeImage> imageConsumer) {
            TextureTarget frame = this.target;
            this.target = null;
            if (frame == null) {
                imageConsumer.accept(null);
                return;
            }
            try {
                // Minecraft runs this callback in its pending GPU tasks, once the GPU finished reading the frame back.
                Screenshot.takeScreenshot(frame, image -> {
                    destroySafely(frame);
                    imageConsumer.accept(image);
                });
            } catch (RuntimeException ex) {
                destroySafely(frame);
                throw ex;
            }
        }

        @Override
        public void release() {
            TextureTarget frame = this.target;
            this.target = null;
            if (frame != null) frame.destroyBuffers();
        }

        private static void destroySafely(@NotNull TextureTarget frame) {
            try {
                frame.destroyBuffers();
            } catch (RuntimeException ex) {
                LOGGER.warn("[FANCYMENU] Failed to release seamless world loading frame copy.", ex);
            }
        }

    }

}
