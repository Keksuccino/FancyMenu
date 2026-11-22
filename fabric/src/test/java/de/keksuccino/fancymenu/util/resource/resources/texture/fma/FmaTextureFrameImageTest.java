package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that FMA frames get decoded completely, also when their PNG is bigger than the {@link MemoryStack} of the decoding thread.
 */
class FmaTextureFrameImageTest {

    @Test
    void decodesAFrameBiggerThanTheMemoryStackOfTheThread() throws IOException {
        byte[] png = FmaTestFiles.framePng(0);
        assertTrue(png.length > MemoryStack.stackGet().getSize(), "The frame must be bigger than the memory stack of the thread");

        try (NativeImage image = FmaTexture.readFrameImage(png)) {
            assertEquals(FmaTestFiles.WIDTH, image.getWidth());
            assertEquals(FmaTestFiles.HEIGHT, image.getHeight());
            BufferedImage expected = ImageIO.read(new ByteArrayInputStream(png));
            int mismatches = 0;
            for (int y = 0; y < FmaTestFiles.HEIGHT; y++) {
                for (int x = 0; x < FmaTestFiles.WIDTH; x++) {
                    if (toAbgr(expected.getRGB(x, y)) != image.getPixelRGBA(x, y)) mismatches++;
                }
            }
            assertEquals(0, mismatches, "Decoded pixels that differ from the frame");
        }
    }

    /**
     * Converts an ARGB pixel into the ABGR order {@link NativeImage#getPixelRGBA(int, int)} returns.
     */
    private static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

}
