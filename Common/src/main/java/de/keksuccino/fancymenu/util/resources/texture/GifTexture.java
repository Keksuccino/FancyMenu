package de.keksuccino.fancymenu.util.resources.texture;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.GifDecoder;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

//TODO hier weiter machen
public class GifTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public @Nullable ResourceLocation getResourceLocation() {
        return null;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return null;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @NotNull
    protected static List<GifFrame> extractFrames(@NotNull File gifFile) {
        List<GifFrame> l = new ArrayList<>();
        try {
            if (gifFile.exists() && gifFile.isFile() && Files.getFileExtension(gifFile.getName()).equalsIgnoreCase("gif")) {
                FileInputStream is = new FileInputStream(gifFile);
                GifDecoder.GifImage gif = GifDecoder.read(is);
                int gifFrameCount = gif.getFrameCount();
                for(int i = 0; i < gifFrameCount; i++) {
                    try {
                        int delay = gif.getDelay(i);
                        BufferedImage image = gif.getFrame(i);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image, "PNG", os);
                        ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
                        l.add(new GifFrame(bis, delay));
                    } catch (Exception ex) {
                        LOGGER.error("###################################################");
                        LOGGER.error("[FANCYMENU] An error happened while trying to read frame " + (i + 1) + " of GIF file '" + gifFile.getPath() + "'!");
                        LOGGER.error("[FANCYMENU] This probably happened because the GIF is somehow corrupted. Using an online tool to edit the GIF could fix this.");
                        LOGGER.error("###################################################");
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return l;
    }

    protected static class GifFrame {
        protected ByteArrayInputStream frameInputStream;
        protected int delay;
        public GifFrame(ByteArrayInputStream frameInputStream, int delay) {
            this.frameInputStream = frameInputStream;
            this.delay = delay;
        }
    }

}
