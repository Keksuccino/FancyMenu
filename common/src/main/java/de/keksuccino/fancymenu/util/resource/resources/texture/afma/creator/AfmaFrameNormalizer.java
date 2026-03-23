package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class AfmaFrameNormalizer {

    protected static final int PNG_HEADER_PROBE_BYTES = 29;
    protected static final byte[] PNG_SIGNATURE = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    @NotNull
    public AfmaPixelFrame loadFrame(@NotNull File file) throws IOException {
        Objects.requireNonNull(file);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (image == null) {
            throw new IOException("Failed to decode AFMA source frame: " + file.getAbsolutePath());
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return new AfmaPixelFrame(width, height, pixels, this.findReusableSourcePngPayload(fileBytes, width, height));
    }

    @NotNull
    public AfmaPixelFrame extractPatch(@NotNull AfmaPixelFrame image, int x, int y, int width, int height) {
        return AfmaPixelFrameHelper.crop(image, x, y, width, height);
    }

    protected @Nullable byte[] findReusableSourcePngPayload(@NotNull byte[] fileBytes, int width, int height) {
        if (fileBytes.length < PNG_HEADER_PROBE_BYTES || !matchesPngSignature(fileBytes)) {
            return null;
        }
        if ((fileBytes[12] != 'I') || (fileBytes[13] != 'H') || (fileBytes[14] != 'D') || (fileBytes[15] != 'R')) {
            return null;
        }

        int sourceWidth = readIntBigEndian(fileBytes, 16);
        int sourceHeight = readIntBigEndian(fileBytes, 20);
        int bitDepth = Byte.toUnsignedInt(fileBytes[24]);
        if (bitDepth != 8 || sourceWidth != width || sourceHeight != height) {
            return null;
        }
        return fileBytes;
    }

    protected static boolean matchesPngSignature(@NotNull byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    protected static int readIntBigEndian(@NotNull byte[] bytes, int offset) {
        return ((bytes[offset] & 255) << 24)
                | ((bytes[offset + 1] & 255) << 16)
                | ((bytes[offset + 2] & 255) << 8)
                | (bytes[offset + 3] & 255);
    }

}
