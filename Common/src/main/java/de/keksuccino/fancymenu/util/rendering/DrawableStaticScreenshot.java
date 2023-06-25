package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DrawableStaticScreenshot {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_drawable_screenshot_texture");

    protected static DynamicTexture currentScreenshot = null;
    protected static ResourceLocation currentScreenshotLocation = null;

    public static void update() {
        createScreenshot();
    }

    protected static void createScreenshot() {
        closeCurrentScreenshot();
        try {
            NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            currentScreenshot = new DynamicTexture(image);
            currentScreenshotLocation = Minecraft.getInstance().getTextureManager().register("drawable_screenshot", currentScreenshot);
        } catch (Exception ex) {
            ex.printStackTrace();
            closeCurrentScreenshot();
        }
    }

    protected static void closeCurrentScreenshot() {
        try {
            if (currentScreenshot != null) currentScreenshot.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        currentScreenshot = null;
        currentScreenshotLocation = null;
    }

    @Nullable
    public static DynamicTexture getScreenshot() {
        return currentScreenshot;
    }

    @NotNull
    public static ResourceLocation getScreenshotLocation() {
        if (currentScreenshotLocation == null) return MISSING;
        return currentScreenshotLocation;
    }

}
