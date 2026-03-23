package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

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
        BufferedImage image = this.createBestBufferedImage(includeAlpha);
        return writeCompressedPng(image);
    }

    protected @NotNull BufferedImage createBestBufferedImage(boolean includeAlpha) {
        BufferedImage indexed = this.createIndexedImage(includeAlpha);
        if (indexed != null) {
            return indexed;
        }

        BufferedImage image = new BufferedImage(this.width, this.height, includeAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, this.width, this.height, this.pixels, 0, this.width);
        return image;
    }

    protected @Nullable BufferedImage createIndexedImage(boolean includeAlpha) {
        LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
        for (int color : this.pixels) {
            int normalizedColor = includeAlpha ? color : (color | 0xFF000000);
            if (!palette.containsKey(normalizedColor)) {
                if (palette.size() >= 256) {
                    return null;
                }
                palette.put(normalizedColor, palette.size());
            }
        }

        int paletteSize = palette.size();
        if (paletteSize <= 0 || paletteSize > 256) {
            return null;
        }

        byte[] reds = new byte[paletteSize];
        byte[] greens = new byte[paletteSize];
        byte[] blues = new byte[paletteSize];
        byte[] alphas = includeAlpha ? new byte[paletteSize] : null;
        for (Map.Entry<Integer, Integer> entry : palette.entrySet()) {
            int color = entry.getKey();
            int index = entry.getValue();
            reds[index] = (byte) ((color >> 16) & 0xFF);
            greens[index] = (byte) ((color >> 8) & 0xFF);
            blues[index] = (byte) (color & 0xFF);
            if (alphas != null) {
                alphas[index] = (byte) ((color >>> 24) & 0xFF);
            }
        }

        IndexColorModel colorModel = (alphas != null)
                ? new IndexColorModel(8, paletteSize, reds, greens, blues, alphas)
                : new IndexColorModel(8, paletteSize, reds, greens, blues);
        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        WritableRaster raster = image.getRaster();
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int color = includeAlpha ? this.pixels[(y * this.width) + x] : (this.pixels[(y * this.width) + x] | 0xFF000000);
                raster.setSample(x, y, 0, palette.get(color));
            }
        }
        return image;
    }

    protected static @NotNull byte[] writeCompressedPng(@NotNull BufferedImage image) throws IOException {
        List<ImageWriter> writers = new ArrayList<>();
        Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("png");
        while (iterator.hasNext()) {
            writers.add(iterator.next());
        }
        if (writers.isEmpty()) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (!ImageIO.write(image, "png", out)) {
                    throw new IOException("Failed to encode AFMA PNG payload");
                }
                return out.toByteArray();
            }
        }

        writers.sort(Comparator.comparing(writer -> writer.getClass().getName()));
        ImageWriter writer = writers.stream()
                .filter(candidate -> candidate.getClass().getName().contains("PNGImageWriter"))
                .findFirst()
                .orElse(writers.get(0));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
            if (imageOut == null) {
                throw new IOException("Failed to create AFMA PNG image output stream");
            }

            writer.setOutput(imageOut);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = writeParam.getCompressionTypes();
                if ((compressionTypes != null) && (compressionTypes.length > 0)) {
                    writeParam.setCompressionType(compressionTypes[0]);
                }
                // Prefer maximum deflate compression because AFMA prioritizes final archive size.
                writeParam.setCompressionQuality(0.0F);
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
            writer.dispose();
            return out.toByteArray();
        } catch (Exception ex) {
            writer.dispose();
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to encode AFMA PNG payload", ex);
        }
    }

    @Override
    public void close() {
    }

}
