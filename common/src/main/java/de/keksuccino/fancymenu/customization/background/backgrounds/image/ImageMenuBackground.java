package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

public class ImageMenuBackground extends MenuBackground {

    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.BLACK;

    public ResourceSupplier<ITexture> textureSupplier;
    public ResourceSupplier<ITexture> fallbackTextureSupplier;
    public boolean slideLeftRight = false;
    public boolean repeat = false;
    protected double slidePos = 0.0D;
    protected boolean slideMoveBack = false;
    protected boolean slideStop = false;
    protected int slideTick = 0;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Variables for the parallax effect
        float parallaxFactor = 0.05f; // Adjust the intensity of the effect
        float scaleFactor = calculateMinimumScaleFactor(parallaxFactor);
        int backgroundWidth = (int) (getScreenWidth() * scaleFactor); // Background image larger than the screen
        int backgroundHeight = (int) (getScreenHeight() * scaleFactor);
        // Get screen dimensions
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        // Calculate the center of the screen
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // Calculate offsets for parallax effect
        float offsetX = (mouseX - centerX) * parallaxFactor;
        float offsetY = (mouseY - centerY) * parallaxFactor;
        // Clamp offsets to ensure the image doesn't move outside its bounds
        offsetX = Math.max(Math.min(offsetX, (backgroundWidth - screenWidth) / 2.0f), -(backgroundWidth - screenWidth) / 2.0f);
        offsetY = Math.max(Math.min(offsetY, (backgroundHeight - screenHeight) / 2.0f), -(backgroundHeight - screenHeight) / 2.0f);
        // Calculate rendering position based on offsets
        float renderPosX = -offsetX; // Move the image left/right based on the offset
        float renderPosY = -offsetY; // Move the image up/down based on the offset

        RenderSystem.enableBlend();
        graphics.fill(RenderType.gui(), 0, 0, getScreenWidth(), getScreenHeight(), BACKGROUND_COLOR.getColorIntWithAlpha(this.opacity));

        ResourceLocation resourceLocation = null;
        ITexture tex = null;
        AspectRatio ratio = new AspectRatio(10, 10);
        if (this.textureSupplier != null) {
            ITexture background = this.textureSupplier.get();
            if (background != null) {
                tex = background;
                ratio = background.getAspectRatio();
                resourceLocation = background.getResourceLocation();
            }
        }
        if ((resourceLocation == null) && (this.fallbackTextureSupplier != null)) {
            ITexture fallback = this.fallbackTextureSupplier.get();
            if (fallback != null) {
                tex = fallback;
                ratio = fallback.getAspectRatio();
                resourceLocation = fallback.getResourceLocation();
            }
        }

        if (resourceLocation != null) {

            RenderSystem.enableBlend();

            if (this.repeat) {
                RenderingUtils.blitRepeat(graphics, resourceLocation, 0, 0, getScreenWidth(), getScreenHeight(), tex.getWidth(), tex.getHeight(), DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
            } else if (this.slideLeftRight) {
                int w = ratio.getAspectRatioWidth(getScreenHeight());
                //Check if background should move to the left or the right side
                if ((slidePos + (w - getScreenWidth())) <= 0) {
                    slideMoveBack = true;
                }
                if (slidePos >= 0) {
                    slideMoveBack = false;
                }
                //Fix pos after resizing
                if (slidePos + (w - getScreenWidth()) < 0) {
                    slidePos = -(w - getScreenWidth());
                }
                if (slidePos > 0) {
                    slidePos = 0;
                }
                if (!slideStop) {
                    if (slideTick >= 1) {
                        slideTick = 0;
                        if (slideMoveBack) {
                            slidePos = slidePos + 0.5;
                        } else {
                            slidePos = slidePos - 0.5;
                        }

                        if (slidePos + (w - getScreenWidth()) == 0) {
                            slideStop = true;
                        }
                        if (slidePos == 0) {
                            slideStop = true;
                        }
                    } else {
                        slideTick++;
                    }
                } else {
                    if (slideTick >= 300) {
                        slideStop = false;
                        slideTick = 0;
                    } else {
                        slideTick++;
                    }
                }
                if (w <= getScreenWidth()) {
                    if (this.keepBackgroundAspectRatio) {
                        this.renderKeepAspectRatio(graphics, ratio, resourceLocation);
                    } else {
                        graphics.blit(RenderType::guiTextured, resourceLocation, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight(), ARGB.white(this.opacity));
                    }
                } else {
                    RenderingUtils.blitF(graphics, RenderType::guiTextured, resourceLocation, (float)slidePos, 0.0F, 0.0F, 0.0F, w, getScreenHeight(), w, getScreenHeight(), ARGB.white(this.opacity));
                }
            } else if (this.keepBackgroundAspectRatio) {
                this.renderKeepAspectRatio(graphics, ratio, resourceLocation);
            } else {
                //graphics.blit(RenderType::guiTextured, resourceLocation, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight(), ARGB.white(this.opacity));
                // Render the background image
                graphics.blit(
                        RenderType::guiTextured,    // Render type
                        resourceLocation,                   // Texture resource
                        (int) renderPosX,          // Render position X
                        (int) renderPosY,          // Render position Y
                        0.0F,                      // needsToBe0_1 (normalized texture X offset, 0-1)
                        0.0F,                      // needsToBe0_2 (normalized texture Y offset, 0-1)
                        screenWidth,               // Render width
                        screenHeight,              // Render height
                        screenWidth,               // Render width again (used for texture scaling)
                        screenHeight,              // Render height again (used for texture scaling)
                        ARGB.white(this.opacity)   // Tint color with opacity
                );
            }

        }

    }

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull AspectRatio ratio, @NotNull ResourceLocation resourceLocation) {
        int[] size = ratio.getAspectRatioSizeByMinimumSize(getScreenWidth(), getScreenHeight());
        int x = 0;
        if (size[0] > getScreenWidth()) {
            x = -((size[0] - getScreenWidth()) / 2);
        }
        int y = 0;
        if (size[1] > getScreenHeight()) {
            y = -((size[1] - getScreenHeight()) / 2);
        }
        graphics.blit(RenderType::guiTextured, resourceLocation, x, y, 0.0F, 0.0F, size[0], size[1], size[0], size[1], ARGB.white(this.opacity));
    }

    // Calculate the minimum scaling factor for the background
    private float calculateMinimumScaleFactor(float parallaxFactor) {

        float maxOffsetX = getScreenWidth() / 2.0f * parallaxFactor;
        float maxOffsetY = getScreenHeight() / 2.0f * parallaxFactor;

        // The background needs to cover the screen plus the maximum offsets
        float scaleFactorX = (getScreenWidth() + 2 * maxOffsetX) / getScreenWidth();
        float scaleFactorY = (getScreenHeight() + 2 * maxOffsetY) / getScreenHeight();

        // Use the larger of the two scaling factors to ensure no black borders
        return Math.max(scaleFactorX, scaleFactorY);

    }

}
