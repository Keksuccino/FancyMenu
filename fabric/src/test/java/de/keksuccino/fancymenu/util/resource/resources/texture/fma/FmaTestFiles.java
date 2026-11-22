package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import org.jetbrains.annotations.NotNull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes small FMA files: ZIP files with a metadata.json and PNG frames of noise that loop forever, every frame shows for 100 ms.
 * Noise doesn't compress, so every frame of 160x160 pixels is bigger than one locked read of the archive.
 */
final class FmaTestFiles {

    static final int WIDTH = 160;
    static final int HEIGHT = 160;

    private FmaTestFiles() {
    }

    @NotNull
    static Path write(@NotNull Path file, int frameCount) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            writeEntry(out, "metadata.json", "{\"loop_count\":0,\"frame_time\":100,\"frame_time_intro\":100}".getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < frameCount; i++) {
                writeEntry(out, "frames/" + i + ".png", framePng(i));
            }
        }
        return file;
    }

    /**
     * The PNG of a frame, as it got written into the file.
     */
    @NotNull
    static byte[] framePng(int frame) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(frame);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, 0xFF000000 | random.nextInt(0x1000000));
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return png.toByteArray();
    }

    private static void writeEntry(@NotNull ZipOutputStream out, @NotNull String name, @NotNull byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

}
