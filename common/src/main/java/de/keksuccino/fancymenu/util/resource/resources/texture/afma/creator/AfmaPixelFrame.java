package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    public @NotNull byte[] asByteArray() throws IOException {
        return this.asByteArray(this.hasAlpha());
    }

    public @NotNull byte[] asByteArray(boolean includeAlpha) throws IOException {
        BufferedImage image = new BufferedImage(this.width, this.height, includeAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, this.width, this.height, this.pixels, 0, this.width);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) {
                throw new IOException("Failed to encode AFMA PNG payload");
            }
            return out.toByteArray();
        }
    }

    @Override
    public void close() {
    }

}
