package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.SerializationHelper;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ImageMenuBackground extends MenuBackground<ImageMenuBackground> {

    public final Property<ResourceSupplier<ITexture>> textureSupplier = putProperty(Property.resourceSupplierProperty(ITexture.class, "image_path", null, "fancymenu.backgrounds.image.configure.choose_image.local", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> fallbackTextureSupplier = putProperty(Property.resourceSupplierProperty(ITexture.class, "fallback_path", null, "fancymenu.backgrounds.image.type.web.fallback", true, true, true, null));
    public final Property<Boolean> slideLeftRight = putProperty(Property.booleanProperty("slide", false, "fancymenu.backgrounds.image.configure.slide"));
    public final Property<Boolean> repeat = putProperty(Property.booleanProperty("repeat_texture", false, "fancymenu.backgrounds.image.configure.repeat"));
    public final Property<Boolean> parallaxEnabled = putProperty(Property.booleanProperty("parallax", false, "fancymenu.backgrounds.image.configure.parallax"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.StringProperty parallaxIntensityXString = putProperty(Property.stringProperty("parallax_intensity_x", "0.02", false, true, "fancymenu.backgrounds.image.configure.parallax_intensity_x"));
    /** Value between 0.0 and 1.0, where 0.0 is no movement and 1.0 is maximum movement **/
    public final Property.StringProperty parallaxIntensityYString = putProperty(Property.stringProperty("parallax_intensity_y", "0.02", false, true, "fancymenu.backgrounds.image.configure.parallax_intensity_y"));
    /** When TRUE, the parallax effect will move in the SAME direction as the mouse, otherwise it moves in the opposite direction **/
    public final Property<Boolean> invertParallax = putProperty(Property.booleanProperty("invert_parallax", false, "fancymenu.backgrounds.image.configure.invert_parallax"));
    public final Property<Boolean> restartAnimatedOnMenuLoad = putProperty(Property.booleanProperty("restart_animated_on_menu_load", false, "fancymenu.backgrounds.image.restart_animated_on_menu_load"));

    protected float currentParallaxIntensityX = -10000.0F;
    protected float currentParallaxIntensityY = -10000.0F;
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
        if (this.restartAnimatedOnMenuLoad.tryGetNonNull()) {
            ResourceSupplier<ITexture> supplier = this.textureSupplier.get();
            if (supplier != null) {
                ITexture tex = supplier.get();
                if (tex instanceof PlayableResource r) {
                    r.stop();
                    r.play();
                }
            }
            ResourceSupplier<ITexture> fallbackSupplier = this.fallbackTextureSupplier.get();
            if (fallbackSupplier != null) {
                ITexture tex = fallbackSupplier.get();
                if (tex instanceof PlayableResource r) {
                    r.stop();
                    r.play();
                }
            }
        }

    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {



    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.currentParallaxIntensityX = SerializationHelper.INSTANCE.deserializeNumber(Float.class, 0.02F, this.parallaxIntensityXString.getString());
        this.currentParallaxIntensityY = SerializationHelper.INSTANCE.deserializeNumber(Float.class, 0.02F, this.parallaxIntensityYString.getString());

        RenderSystem.enableBlend();

        ResourceLocation resourceLocation = null;
        ITexture tex = null;
        AspectRatio ratio = new AspectRatio(10, 10);
        ResourceSupplier<ITexture> supplier = this.textureSupplier.get();
        if (supplier != null) {
            ITexture background = supplier.get();
            if (background != null) {
                tex = background;
                ratio = background.getAspectRatio();
                resourceLocation = background.getResourceLocation();
            }
        }
        ResourceSupplier<ITexture> fallbackSupplier = this.fallbackTextureSupplier.get();
        if ((resourceLocation == null) && (fallbackSupplier != null)) {
            ITexture fallback = fallbackSupplier.get();
            if (fallback != null) {
                tex = fallback;
                ratio = fallback.getAspectRatio();
                resourceLocation = fallback.getResourceLocation();
            }
        }

        if (resourceLocation != null) {
            float[] parallaxOffset = calculateParallaxOffset(mouseX, mouseY);
            if (this.repeat.tryGetNonNull()) {
                renderRepeatBackground(graphics, resourceLocation, tex, parallaxOffset);
            } else if (this.slideLeftRight.tryGetNonNull() && !this.parallaxEnabled.tryGetNonNull()) {
                renderSlideBackground(graphics, ratio, resourceLocation, parallaxOffset);
            } else if (this.keepBackgroundAspectRatio) {
                renderKeepAspectRatio(graphics, ratio, resourceLocation, parallaxOffset);
            } else {
                renderFullScreen(graphics, resourceLocation, parallaxOffset);
            }
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    protected float[] calculateParallaxOffset(int mouseX, int mouseY) {
        if (!this.parallaxEnabled.tryGetNonNull()) {
            return new float[]{0, 0};
        }

        // Calculate mouse position as a percentage from the center of the screen
        float mouseXPercent = (2.0f * mouseX / getScreenWidth()) - 1.0f;
        float mouseYPercent = (2.0f * mouseY / getScreenHeight()) - 1.0f;

        // Apply inversion if enabled
        float directionMultiplier = this.invertParallax.tryGetNonNull() ? 1.0f : -1.0f;

        // Calculate offset based on screen dimensions and center-adjusted mouse position
        float xOffset = directionMultiplier * this.currentParallaxIntensityX * mouseXPercent * getScreenWidth() * 0.5f;
        float yOffset = directionMultiplier * this.currentParallaxIntensityY * mouseYPercent * getScreenHeight() * 0.5f;

        return new float[]{xOffset, yOffset};

    }

    protected void renderRepeatBackground(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, ITexture tex, float[] parallaxOffset) {

        graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);

        if (this.parallaxEnabled.tryGetNonNull()) {
            // Create expanded area for parallax movement
            int expandedWidth = (int)(getScreenWidth() * (1.0F + this.currentParallaxIntensityX));
            int expandedHeight = (int)(getScreenHeight() * (1.0F + this.currentParallaxIntensityY));

            // Center the expanded area and apply parallax offset
            int baseX = -((expandedWidth - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            int baseY = -((expandedHeight - getScreenHeight()) / 2) + (int)parallaxOffset[1];

            RenderingUtils.blitRepeat(graphics, resourceLocation, baseX, baseY, expandedWidth, expandedHeight, tex.getWidth(), tex.getHeight());
        } else {
            RenderingUtils.blitRepeat(graphics, resourceLocation, 0, 0, getScreenWidth(), getScreenHeight(), tex.getWidth(), tex.getHeight());
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    protected void renderSlideBackground(@NotNull GuiGraphics graphics, @NotNull AspectRatio ratio, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        int w = ratio.getAspectRatioWidth(getScreenHeight());
        handleSlideAnimation(w);
        graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
        if (w <= getScreenWidth()) {
            if (this.keepBackgroundAspectRatio) {
                renderKeepAspectRatio(graphics, ratio, resourceLocation, parallaxOffset);
            } else {
                renderFullScreen(graphics, resourceLocation, parallaxOffset);
            }
        } else {
            float finalX = (float)this.slidePos;
            RenderingUtils.blitF(graphics, resourceLocation, finalX, parallaxOffset[1], 0.0F, 0.0F, w, getScreenHeight(), w, getScreenHeight());
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderKeepAspectRatio(@NotNull GuiGraphics graphics, @NotNull AspectRatio ratio, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        // Calculate base size with reduced parallax expansion
        boolean parallax = this.parallaxEnabled.tryGetNonNull();
        float parallaxScaleX = parallax ? (1.0F + this.currentParallaxIntensityX) : 1.0F;
        float parallaxScaleY = parallax ? (1.0F + this.currentParallaxIntensityY) : 1.0F;
        int[] baseSize = ratio.getAspectRatioSizeByMinimumSize(
                (int)(getScreenWidth() * parallaxScaleX),
                (int)(getScreenHeight() * parallaxScaleY)
        );

        // Calculate centered position with adjusted parallax offset
        int x = (getScreenWidth() - baseSize[0]) / 2 + (int)parallaxOffset[0];
        int y = (getScreenHeight() - baseSize[1]) / 2 + (int)parallaxOffset[1];

        graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);

        graphics.blit(resourceLocation, x, y, 0.0F, 0.0F, baseSize[0], baseSize[1], baseSize[0], baseSize[1]);

        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderFullScreen(@NotNull GuiGraphics graphics, @NotNull ResourceLocation resourceLocation, float[] parallaxOffset) {
        graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
        if (this.parallaxEnabled.tryGetNonNull()) {
            // Reduce the expansion amount for parallax
            int expandedWidth = (int)(getScreenWidth() * (1.0F + this.currentParallaxIntensityX));
            int expandedHeight = (int)(getScreenHeight() * (1.0F + this.currentParallaxIntensityY));

            // Center the expanded area and apply parallax offset
            int x = -((expandedWidth - getScreenWidth()) / 2) + (int)parallaxOffset[0];
            int y = -((expandedHeight - getScreenHeight()) / 2) + (int)parallaxOffset[1];

            graphics.blit(resourceLocation, x, y, 0.0F, 0.0F, expandedWidth, expandedHeight, expandedWidth, expandedHeight);
        } else {
            graphics.blit(resourceLocation, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }
        RenderingUtils.resetShaderColor(graphics);
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
