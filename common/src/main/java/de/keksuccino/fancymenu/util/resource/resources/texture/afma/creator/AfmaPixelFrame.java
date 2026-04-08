package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class AfmaPixelFrame implements AutoCloseable {

    private final int width;
    private final int height;
    private final @NotNull int[] pixels;

    public AfmaPixelFrame(int width, int height, @NotNull int[] pixels) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("AFMA frame dimensions must be greater than zero");
        }

        Objects.requireNonNull(pixels);
        if (pixels.length != (width * height)) {
            throw new IllegalArgumentException("AFMA frame pixel buffer size does not match dimensions");
        }

        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getPixelRGBA(int x, int y) {
        return this.pixels[(y * this.width) + x];
    }

    public void setPixelRGBA(int x, int y, int color) {
        this.pixels[(y * this.width) + x] = color;
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
    }

}
