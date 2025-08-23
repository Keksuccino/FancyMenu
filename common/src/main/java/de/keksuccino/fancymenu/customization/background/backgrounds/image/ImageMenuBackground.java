package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
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
    public boolean parallaxEnabled = false;
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    @NotNull
    public String parallaxIntensityString = "0.02";
    public float lastParallaxIntensity = -10000.0F;
    /** When TRUE, the parallax effect will move in the SAME direction as the mouse, otherwise it moves in the opposite direction **/
    public boolean invertParallax = false;
    public boolean restartAnimatedOnMenuLoad = false;

    protected double slidePos = 0.0D;
    protected boolean slideMoveBack = false;
    protected boolean slideStop = false;
    protected int slideTick = 0;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void onOpenScreen() {

        super.onOpenScreen();

        // Restart animated textures on menu load if enabled
        if (this.restartAnimatedOnMenuLoad) {
            if (this.textureSupplier != null) {
                ITexture tex = this.textureSupplier.get();
                if (tex instanceof PlayableResource r) {
                    r.stop();
                    r.play();
                }
            }
            if (this.fallbackTextureSupplier != null) {
                ITexture tex = this.fallbackTextureSupplier.get();
                if (tex instanceof PlayableResource r) {
                    r.stop();
                    r.play();
                }
            }
        }

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.lastParallaxIntensity = SerializationUtils.deserializeNumber(Float.class, 0.02F, PlaceholderParser.replacePlaceholders(this.parallaxIntensityString));

        RenderSystem.enableBlend();

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

        // Calculate mouse position as a percentage from the center of the screen
        float mouseXPercent = (2.0f * mouseX / getScreenWidth()) - 1.0f;
        float mouseYPercent = (2.0f * mouseY / getScreenHeight()) - 1.0f;

        // Apply inversion if enabled
        float directionMultiplier = invertParallax ? 1.0f : -1.0f;

        // Calculate offset based on screen dimensions and center-adjusted mouse position
        float xOffset = directionMultiplier * this.lastParallaxIntensity * mouseXPercent * getScreenWidth() * 0.5f;
        float yOffset = directionMultiplier * this.lastParallaxIntensity * mouseYPercent * getScreenHeight() * 0.5f;

        return new float[]{xOffset, yOffset};
    }

    protected void renderRepeatBackground(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, ITexture tex, float[] parallaxOffset) {
        if (parallaxEnabled) {
            // Create expanded area for parallax movement
            int expandedWidth = (int)(getScreenWidth() * (1.0F + this.lastParallaxIntensity));
            int expandedHeight = (int)(getScreenHeight() * (1.0F + this.lastParallaxIntensity));

            // Center the expanded area and apply parallax offset
            int baseX = -((expandedWidth - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            int baseY = -((expandedHeight - getScreenHeight()) / 2) + (int)parallaxOffset[1];

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
        // Calculate base size with reduced parallax expansion
        float parallaxScale = parallaxEnabled ? (1.0F + this.lastParallaxIntensity) : 1.0F;
        int[] baseSize = ratio.getAspectRatioSizeByMinimumSize(
                (int)(getScreenWidth() * parallaxScale),
                (int)(getScreenHeight() * parallaxScale)
        );

        // Calculate centered position with adjusted parallax offset
        int x = (getScreenWidth() - baseSize[0]) / 2 + (int)parallaxOffset[0];
        int y = (getScreenHeight() - baseSize[1]) / 2 + (int)parallaxOffset[1];

        graphics.blit(RenderType::guiTextured, resourceLocation,
                x, y, 0.0F, 0.0F,
                baseSize[0], baseSize[1],
                baseSize[0], baseSize[1],
                ARGB.white(this.opacity));
    }

    protected void renderFullScreen(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        if (parallaxEnabled) {
            // Reduce the expansion amount for parallax
            int expandedWidth = (int)(getScreenWidth() * (1.0F + this.lastParallaxIntensity));
            int expandedHeight = (int)(getScreenHeight() * (1.0F + this.lastParallaxIntensity));

            // Center the expanded area and apply parallax offset
            int x = -((expandedWidth - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            int y = -((expandedHeight - getScreenHeight()) / 2) + (int)parallaxOffset[1];

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