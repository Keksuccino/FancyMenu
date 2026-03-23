package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class AfmaFrameNormalizer {

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file) throws IOException {
        Objects.requireNonNull(file);
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath());
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return new AfmaPixelFrame(width, height, pixels);
    }

    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height) {
        return AfmaPixelFrameHelper.crop(image, x, y, width, height);
    }

}
