package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class AfmaPixelFrame implements AutoCloseable {

    private final int width;
    private final int height;
    private final @NotNull int[] pixels;
    @Nullable
    private final AfmaFastPixelBufferPool pixelBufferPool;
    private boolean returnedToPool;

    public AfmaPixelFrame(int width, int height, @NotNull int[] pixels) {
        this(width, height, pixels, null);
    }

    AfmaPixelFrame(int width, int height, @NotNull int[] pixels, @Nullable AfmaFastPixelBufferPool pixelBufferPool) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("AFMA frame dimensions must be greater than zero");
        }

        Objects.requireNonNull(pixels);
        if (pixels.length != AfmaPixelFrameHelper.pixelCount(width, height)) {
            throw new IllegalArgumentException("AFMA frame pixel buffer size does not match dimensions");
        }

        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.pixelBufferPool = pixelBufferPool;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getPixelCount() {
        return this.pixels.length;
    }

    public int getRowStride() {
        return this.width;
    }

    public int getPixelIndex(int x, int y) {
        return (y * this.width) + x;
    }

    public int getRowOffset(int y) {
        return y * this.width;
    }

    public int getPixelRGBA(int x, int y) {
        return this.pixels[this.getPixelIndex(x, y)];
    }

    public void setPixelRGBA(int x, int y, int color) {
        this.pixels[this.getPixelIndex(x, y)] = color;
    }

    @NotNull
    public PixelView fullView() {
        return new PixelView(this.pixels, 0, this.width, this.height, this.width);
    }

    @NotNull
    public PixelView view(int x, int y, int width, int height) {
        AfmaPixelFrameHelper.validateContainedRegion(this, x, y, width, height);
        return new PixelView(this.pixels, this.getPixelIndex(x, y), width, height, this.width);
    }

    public void copyPixelsTo(@NotNull int[] targetPixels) {
        this.copyPixelsTo(targetPixels, 0);
    }

    public void copyPixelsTo(@NotNull int[] targetPixels, int targetOffset) {
        Objects.requireNonNull(targetPixels);
        if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < this.pixels.length)) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the source frame");
        }
        System.arraycopy(this.pixels, 0, targetPixels, targetOffset, this.pixels.length);
    }

    public void copyRowTo(int y, int srcX, @NotNull int[] targetPixels, int targetOffset, int length) {
        Objects.requireNonNull(targetPixels);
        if ((y < 0) || (y >= this.height)) {
            throw new IndexOutOfBoundsException("AFMA row index out of bounds");
        }
        if ((srcX < 0) || (length < 0) || ((srcX + length) > this.width)) {
            throw new IndexOutOfBoundsException("AFMA row copy exceeds frame bounds");
        }
        if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < length)) {
            throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the requested row copy");
        }
        System.arraycopy(this.pixels, this.getPixelIndex(srcX, y), targetPixels, targetOffset, length);
    }

    public boolean hasAlpha() {
        for (int color : this.pixels) {
            if (((color >>> 24) & 0xFF) != 0xFF) {
                return true;
            }
        }
        return false;
    }

    public @NotNull AfmaPixelFrame copy() {
        return new AfmaPixelFrame(this.width, this.height, Arrays.copyOf(this.pixels, this.pixels.length));
    }

    public @NotNull int[] copyPixels() {
        return Arrays.copyOf(this.pixels, this.pixels.length);
    }

    /**
     * Exposes the backing pixels for read-only hot paths that need to avoid extra copies.
     */
    public @NotNull int[] getPixelsUnsafe() {
        return this.pixels;
    }

    public @NotNull byte[] asByteArray() throws IOException {
        return AfmaBinIntraPayloadHelper.encodePayload(this.width, this.height, this.pixels);
    }

    @Override
    public void close() {
        if (!this.returnedToPool && (this.pixelBufferPool != null)) {
            this.returnedToPool = true;
            this.pixelBufferPool.releasePixels(this.pixels);
        }
    }

    public static final class PixelView {

        @NotNull
        private final int[] pixels;
        private final int offset;
        private final int width;
        private final int height;
        private final int stride;

        PixelView(@NotNull int[] pixels, int offset, int width, int height, int stride) {
            this.pixels = Objects.requireNonNull(pixels);
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.stride = stride;
        }

        @NotNull
        public int[] pixels() {
            return this.pixels;
        }

        public int offset() {
            return this.offset;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }

        public int stride() {
            return this.stride;
        }

        public int getPixelRGBA(int x, int y) {
            if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) {
                throw new IndexOutOfBoundsException("AFMA pixel view coordinates out of bounds");
            }
            return this.pixels[this.offset + (y * this.stride) + x];
        }

        public void copyRowTo(int y, int srcX, @NotNull int[] targetPixels, int targetOffset, int length) {
            Objects.requireNonNull(targetPixels);
            if ((y < 0) || (y >= this.height)) {
                throw new IndexOutOfBoundsException("AFMA pixel view row index out of bounds");
            }
            if ((srcX < 0) || (length < 0) || ((srcX + length) > this.width)) {
                throw new IndexOutOfBoundsException("AFMA pixel view row copy exceeds bounds");
            }
            if ((targetOffset < 0) || ((targetPixels.length - targetOffset) < length)) {
                throw new IllegalArgumentException("AFMA target pixel buffer is smaller than the requested row copy");
            }
            System.arraycopy(this.pixels, this.offset + (y * this.stride) + srcX, targetPixels, targetOffset, length);
        }
    }

}
