package de.keksuccino.fancymenu.util.resource.resources.video.clip;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import java.awt.image.BufferedImage;

public class MP4FrameImage extends DynamicTexture {

    @NotNull
    public static MP4FrameImage build(@NotNull ResourceLocation resourceLocation, int videoWidth, int videoHeight) {
        MP4FrameImage image = new MP4FrameImage(new NativeImage(videoWidth, videoHeight, false));
        Minecraft.getInstance().getTextureManager().register(resourceLocation, image);
        return image;
    }

    protected MP4FrameImage(@NotNull NativeImage image) {
        super(image);
    }

    public void upload(@NotNull BufferedImage bufferedImage) {
        if (this.getPixels() == null) return;
        for (int x = 0; x < Math.min(this.getPixels().getWidth(), bufferedImage.getWidth()); x++) {
            for (int y = 0; y < Math.min(this.getPixels().getHeight(), bufferedImage.getHeight()); y++) {
                int rgb = bufferedImage.getRGB(x, y);
                int r = FastColor.ARGB32.red(rgb);
                int g = FastColor.ARGB32.green(rgb);
                int b = FastColor.ARGB32.blue(rgb);
                this.getPixels().setPixelRGBA(x, y, FastColor.ABGR32.color(255, b, g, r));
            }
        }
        this.upload();
    }

}
