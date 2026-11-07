package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Turns any image into a square profile icon of {@link #ICON_SIZE} pixels. Non-square images get cropped to their center,
 * so users can pick any image without preparing it first.
 */
public final class ProfileIconProcessor {

    public static final int ICON_SIZE = 64;
    /** Big images get subsampled while decoding, so they never get fully loaded into memory. */
    private static final int MAX_DECODED_SIDE = 1024;

    private ProfileIconProcessor() {
    }

    /**
     * Reads an image, crops it to a centered square, scales it to the icon size and writes it as PNG.
     *
     * @throws IOException if the file is no readable image.
     */
    public static void writeIcon(@NotNull Path sourceImage, @NotNull Path targetPng) throws IOException {
        BufferedImage square = readCenteredSquare(sourceImage);
        BufferedImage icon = scaleToIconSize(square);
        Path tempFile = targetPng.resolveSibling(targetPng.getFileName().toString() + ".tmp");
        try {
            if (!ImageIO.write(icon, "png", tempFile.toFile())) throw new IOException("No PNG writer available!");
            ProfileStorage.moveReplacing(tempFile, targetPng);
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(tempFile);
            throw ex;
        }
    }

    @NotNull
    private static BufferedImage readCenteredSquare(@NotNull Path sourceImage) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(sourceImage.toFile())) {
            if (input == null) throw new IOException("Can't open image: " + sourceImage);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image format: " + sourceImage);
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int side = Math.min(width, height);
                if (side <= 0) throw new IOException("Image has no pixels: " + sourceImage);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceRegion(new Rectangle((width - side) / 2, (height - side) / 2, side, side));
                int subsampling = Math.max(1, side / MAX_DECODED_SIDE);
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                BufferedImage image = reader.read(0, param);
                if (image == null) throw new IOException("Failed to decode image: " + sourceImage);
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Downscaling averages all covered source pixels with premultiplied alpha, which keeps edges clean and avoids dark fringes around
     * transparent areas. Smaller images get upscaled with nearest-neighbor sampling, so pixel art stays crisp.
     */
    @NotNull
    static BufferedImage scaleToIconSize(@NotNull BufferedImage source) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, sourceWidth, sourceHeight, null, 0, sourceWidth);
        int[] targetPixels = new int[ICON_SIZE * ICON_SIZE];
        double scaleX = (double) sourceWidth / ICON_SIZE;
        double scaleY = (double) sourceHeight / ICON_SIZE;
        boolean upscale = (sourceWidth < ICON_SIZE) || (sourceHeight < ICON_SIZE);
        for (int y = 0; y < ICON_SIZE; y++) {
            for (int x = 0; x < ICON_SIZE; x++) {
                if (upscale) {
                    int sx = Math.min(sourceWidth - 1, (int) (x * scaleX));
                    int sy = Math.min(sourceHeight - 1, (int) (y * scaleY));
                    targetPixels[(y * ICON_SIZE) + x] = sourcePixels[(sy * sourceWidth) + sx];
                } else {
                    targetPixels[(y * ICON_SIZE) + x] = averageArea(sourcePixels, sourceWidth, sourceHeight, x * scaleX, y * scaleY, (x + 1) * scaleX, (y + 1) * scaleY);
                }
            }
        }
        BufferedImage target = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        target.setRGB(0, 0, ICON_SIZE, ICON_SIZE, targetPixels, 0, ICON_SIZE);
        return target;
    }

    private static int averageArea(@NotNull int[] pixels, int width, int height, double x0, double y0, double x1, double y1) {
        double alpha = 0, red = 0, green = 0, blue = 0, totalWeight = 0;
        int startY = (int) Math.floor(y0);
        int endY = Math.min(height, (int) Math.ceil(y1));
        int startX = (int) Math.floor(x0);
        int endX = Math.min(width, (int) Math.ceil(x1));
        for (int sy = startY; sy < endY; sy++) {
            double weightY = Math.min(y1, sy + 1) - Math.max(y0, sy);
            if (weightY <= 0) continue;
            for (int sx = startX; sx < endX; sx++) {
                double weightX = Math.min(x1, sx + 1) - Math.max(x0, sx);
                if (weightX <= 0) continue;
                double weight = weightX * weightY;
                int argb = pixels[(sy * width) + sx];
                double a = ((argb >>> 24) & 0xFF) / 255.0;
                alpha += a * weight;
                red += ((argb >> 16) & 0xFF) * a * weight;
                green += ((argb >> 8) & 0xFF) * a * weight;
                blue += (argb & 0xFF) * a * weight;
                totalWeight += weight;
            }
        }
        if ((totalWeight <= 0) || (alpha <= 0)) return 0;
        int a = clampColor((alpha / totalWeight) * 255.0);
        int r = clampColor(red / alpha);
        int g = clampColor(green / alpha);
        int b = clampColor(blue / alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampColor(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

}
