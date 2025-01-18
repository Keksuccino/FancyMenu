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
    public boolean parallaxEnabled = true;
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public float parallaxIntensity = 0.02F;

    protected double slidePos = 0.0D;
    protected boolean slideMoveBack = false;
    protected boolean slideStop = false;
    protected int slideTick = 0;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

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
            // Calculate parallax offsets if enabled
            float[] parallaxOffset = calculateParallaxOffset(mouseX, mouseY);
            if (this.repeat) {
                renderRepeatBackground(graphics, resourceLocation, tex, parallaxOffset);
            } else if (this.slideLeftRight && !this.parallaxEnabled) {
                renderSlideBackground(graphics, ratio, resourceLocation, parallaxOffset);
            } else if (this.keepBackgroundAspectRatio) {
                renderKeepAspectRatio(graphics, ratio, resourceLocation, parallaxOffset);
            } else {
                renderFullScreen(graphics, resourceLocation, parallaxOffset);
            }
        }

    }

    protected float[] calculateParallaxOffset(int mouseX, int mouseY) {

        if (!parallaxEnabled) {
            return new float[]{0, 0};
        }

        float mouseXPercent = (float)mouseX / getScreenWidth();
        float mouseYPercent = (float)mouseY / getScreenHeight();

        float xOffset = -parallaxIntensity * mouseXPercent * getScreenWidth();
        float yOffset = -parallaxIntensity * mouseYPercent * getScreenHeight();

        return new float[]{xOffset, yOffset};

    }

    protected void renderRepeatBackground(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, ITexture tex, float[] parallaxOffset) {
        if (parallaxEnabled) {
            // For parallax with repeat, we'll create a slightly larger area and offset it
            int expandedWidth = (int)(getScreenWidth() * (1 + parallaxIntensity * 2));
            int expandedHeight = (int)(getScreenHeight() * (1 + parallaxIntensity * 2));

            // Center the expanded area
            int baseX = (int)((expandedWidth - getScreenWidth()) / -2.0f + parallaxOffset[0]);
            int baseY = (int)((expandedHeight - getScreenHeight()) / -2.0f + parallaxOffset[1]);

            RenderingUtils.blitRepeat(graphics, resourceLocation, baseX, baseY,
                    expandedWidth, expandedHeight,
                    tex.getWidth(), tex.getHeight(),
                    DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
        } else {
            RenderingUtils.blitRepeat(graphics, resourceLocation, 0, 0,
                    getScreenWidth(), getScreenHeight(),
                    tex.getWidth(), tex.getHeight(),
                    DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
        }
    }

    protected void renderSlideBackground(@NotNull GuiGraphics graphics, @NotNull AspectRatio ratio, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        int w = ratio.getAspectRatioWidth(getScreenHeight());
        handleSlideAnimation(w);
        if (w <= getScreenWidth()) {
            if (this.keepBackgroundAspectRatio) {
                renderKeepAspectRatio(graphics, ratio, resourceLocation, parallaxOffset);
            } else {
                renderFullScreen(graphics, resourceLocation, parallaxOffset);
            }
        } else {
            float finalX = (float)slidePos;
            RenderingUtils.blitF(graphics, RenderType::guiTextured, resourceLocation,
                    finalX, parallaxOffset[1], 0.0F, 0.0F,
                    w, getScreenHeight(), w, getScreenHeight(),
                    ARGB.white(this.opacity));
        }
    }

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull AspectRatio ratio, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {

        // Calculate base size with extra space for parallax movement
        float parallaxScale = parallaxEnabled ? (1 + parallaxIntensity * 2) : 1;
        int[] baseSize = ratio.getAspectRatioSizeByMinimumSize(
                (int)(getScreenWidth() * parallaxScale),
                (int)(getScreenHeight() * parallaxScale)
        );

        // Calculate centered position
        int x = -((baseSize[0] - getScreenWidth()) / 2);
        int y = -((baseSize[1] - getScreenHeight()) / 2);

        // Apply parallax offset
        x += (int)parallaxOffset[0];
        y += (int)parallaxOffset[1];

        graphics.blit(RenderType::guiTextured, resourceLocation,
                x, y, 0.0F, 0.0F,
                baseSize[0], baseSize[1],
                baseSize[0], baseSize[1],
                ARGB.white(this.opacity));

    }

    protected void renderFullScreen(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        if (parallaxEnabled) {
            int expandedWidth = (int)(getScreenWidth() * (1 + parallaxIntensity * 2));
            int expandedHeight = (int)(getScreenHeight() * (1 + parallaxIntensity * 2));
            int x = (int)((expandedWidth - getScreenWidth()) / -2.0f + parallaxOffset[0]);
            int y = (int)((expandedHeight - getScreenHeight()) / -2.0f + parallaxOffset[1]);
            graphics.blit(RenderType::guiTextured, resourceLocation,
                    x, y, 0.0F, 0.0F,
                    expandedWidth, expandedHeight,
                    expandedWidth, expandedHeight,
                    ARGB.white(this.opacity));
        } else {
            graphics.blit(RenderType::guiTextured, resourceLocation,
                    0, 0, 0.0F, 0.0F,
                    getScreenWidth(), getScreenHeight(),
                    getScreenWidth(), getScreenHeight(),
                    ARGB.white(this.opacity));
        }
    }

    protected void handleSlideAnimation(int backgroundWidth) {
        if ((slidePos + (backgroundWidth - getScreenWidth())) <= 0) {
            slideMoveBack = true;
        }
        if (slidePos >= 0) {
            slideMoveBack = false;
        }

        if (slidePos + (backgroundWidth - getScreenWidth()) < 0) {
            slidePos = -(backgroundWidth - getScreenWidth());
        }
        if (slidePos > 0) {
            slidePos = 0;
        }

        if (!slideStop) {
            if (slideTick >= 1) {
                slideTick = 0;
                slidePos += slideMoveBack ? 0.5 : -0.5;

                if (slidePos + (backgroundWidth - getScreenWidth()) == 0 || slidePos == 0) {
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
    }

}