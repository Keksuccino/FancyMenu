package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileIconProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void wideImagesGetCroppedToTheirCenter() throws Exception {
        BufferedImage wide = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 300; x++) {
                wide.setRGB(x, y, (x < 100) ? 0xFFFF0000 : ((x < 200) ? 0xFF00FF00 : 0xFF0000FF));
            }
        }
        Path source = this.tempDir.resolve("wide.png");
        ImageIO.write(wide, "png", source.toFile());
        Path icon = this.tempDir.resolve("icon.png");

        ProfileIconProcessor.writeIcon(source, icon);

        BufferedImage result = ImageIO.read(icon.toFile());
        assertEquals(ProfileIconProcessor.ICON_SIZE, result.getWidth());
        assertEquals(ProfileIconProcessor.ICON_SIZE, result.getHeight());
        assertEquals(0xFF00FF00, result.getRGB(0, 0));
        assertEquals(0xFF00FF00, result.getRGB(63, 63));
        assertFalse(Files.exists(this.tempDir.resolve("icon.png.tmp")));
    }

    @Test
    void smallImagesStayCrisp() {
        BufferedImage pixelArt = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        pixelArt.setRGB(0, 0, 0xFFFFFFFF);
        pixelArt.setRGB(1, 0, 0xFF000000);
        pixelArt.setRGB(0, 1, 0xFF000000);
        pixelArt.setRGB(1, 1, 0xFFFFFFFF);

        BufferedImage result = ProfileIconProcessor.scaleToIconSize(pixelArt);

        assertEquals(0xFFFFFFFF, result.getRGB(31, 31));
        assertEquals(0xFF000000, result.getRGB(32, 31));
        assertEquals(0xFF000000, result.getRGB(31, 32));
        assertEquals(0xFFFFFFFF, result.getRGB(32, 32));
    }

    @Test
    void transparentPixelsDontDarkenTheirNeighbors() {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                // Fully transparent black next to opaque red, which would turn dark red without premultiplied alpha
                image.setRGB(x, y, ((x % 2) == 0) ? 0xFFFF0000 : 0x00000000);
            }
        }

        int pixel = ProfileIconProcessor.scaleToIconSize(image).getRGB(10, 10);

        assertEquals(0xFF, (pixel >> 16) & 0xFF);
        assertEquals(128, (pixel >>> 24) & 0xFF, 1);
    }

    @Test
    void filesThatAreNoImagesFail() throws Exception {
        Path notAnImage = Files.writeString(this.tempDir.resolve("text.png"), "no image");

        assertThrows(Exception.class, () -> ProfileIconProcessor.writeIcon(notAnImage, this.tempDir.resolve("icon.png")));
        assertFalse(Files.exists(this.tempDir.resolve("icon.png")));
    }

}
