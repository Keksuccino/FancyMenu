package de.keksuccino.fancymenu.customization.element.elements.image;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothImageRectangleRenderer;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;

public class ImageElement extends AbstractElement {

    public final Property<ResourceSupplier<ITexture>> textureSupplier = putProperty(Property.resourceSupplierProperty(ITexture.class, "source", null, "fancymenu.elements.image.set_source", true, true, true, null));
    public final Property.BooleanProperty repeat = putProperty(Property.booleanProperty("repeat_texture", false, "fancymenu.elements.image.repeat"));
    public final Property.BooleanProperty nineSlice = putProperty(Property.booleanProperty("nine_slice_texture", false, "fancymenu.elements.image.nine_slice"));
    public final Property.IntegerProperty nineSliceBorderX = putProperty(Property.integerProperty("nine_slice_texture_border_x", 5, "fancymenu.elements.image.nine_slice.border_x", Property.NumericInputBehavior.<Integer>builder().rangeInput(1, 100).build()));
    public final Property.IntegerProperty nineSliceBorderY = putProperty(Property.integerProperty("nine_slice_texture_border_y", 5, "fancymenu.elements.image.nine_slice.border_y", Property.NumericInputBehavior.<Integer>builder().rangeInput(1, 100).build()));
    public final Property.BooleanProperty restartAnimatedOnMenuLoad = putProperty(Property.booleanProperty("restart_animated_on_menu_load", false, "fancymenu.elements.image.restart_animated_on_menu_load"));
    public final Property.ColorProperty imageTint = putProperty(Property.hexColorProperty("image_tint", "#FFFFFF", true, "fancymenu.elements.image.tint"));
    public final Property.FloatProperty roundingRadiusTopLeft = putProperty(Property.floatProperty("rounding_radius_top_left", 0, "fancymenu.elements.image.rounding_radius.top_left", Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 100.0F).build()));
    public final Property.FloatProperty roundingRadiusTopRight = putProperty(Property.floatProperty("rounding_radius_top_right", 0, "fancymenu.elements.image.rounding_radius.top_right", Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 100.0F).build()));
    public final Property.FloatProperty roundingRadiusBottomRight = putProperty(Property.floatProperty("rounding_radius_bottom_right", 0, "fancymenu.elements.image.rounding_radius.bottom_right", Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 100.0F).build()));
    public final Property.FloatProperty roundingRadiusBottomLeft = putProperty(Property.floatProperty("rounding_radius_bottom_left", 0, "fancymenu.elements.image.rounding_radius.bottom_left", Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 100.0F).build()));

    @Nullable
    protected DrawableColor currentImageTint;
    protected float resolvedRoundingRadiusTopLeft;
    protected float resolvedRoundingRadiusTopRight;
    protected float resolvedRoundingRadiusBottomRight;
    protected float resolvedRoundingRadiusBottomLeft;

    public ImageElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void onOpenScreen() {

        super.onOpenScreen();

        // Restart animated textures on menu load if enabled
        if (this.restartAnimatedOnMenuLoad.getBoolean()) {
            ResourceSupplier<ITexture> supplier = this.textureSupplier.get();
            if (supplier != null) {
                ITexture texture = supplier.get();
                if (texture instanceof PlayableResource r) {
                    r.stop();
                    r.play();
                }
            }
        }

    }

    protected void tickImageTint() {
        this.currentImageTint = this.imageTint.getDrawable();
    }

    protected void tickRoundingRadius() {
        this.resolvedRoundingRadiusTopLeft = this.roundingRadiusTopLeft.getFloat();
        this.resolvedRoundingRadiusTopRight = this.roundingRadiusTopRight.getFloat();
        this.resolvedRoundingRadiusBottomRight = this.roundingRadiusBottomRight.getFloat();
        this.resolvedRoundingRadiusBottomLeft = this.roundingRadiusBottomLeft.getFloat();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.tickImageTint();
            this.tickRoundingRadius();
            if (this.currentImageTint == null) return;

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();

            RenderSystem.enableBlend();

            ITexture t = this.getTextureResource();
            if ((t != null) && t.isReady()) {
                ResourceLocation loc = t.getResourceLocation();
                if (loc != null) {
                    if (this.repeat.getBoolean()) {
                        this.currentImageTint.setAsShaderColor(graphics, this.opacity);
                        RenderingUtils.blitRepeat(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight());
                        this.currentImageTint.resetShaderColor(graphics);
                    } else if (this.nineSlice.getBoolean()) {
                        this.currentImageTint.setAsShaderColor(graphics, this.opacity);
                        int borderX = this.nineSliceBorderX.getInteger();
                        int borderY = this.nineSliceBorderY.getInteger();
                        RenderingUtils.blitNineSlicedTexture(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight(), borderY, borderX, borderY, borderX);
                        this.currentImageTint.resetShaderColor(graphics);
                    } else {
                        int color = resolveTintColor(this.currentImageTint, this.opacity);
                        SmoothImageRectangleRenderer.renderSmoothImageRectRoundAllCornersScaled(
                                graphics,
                                loc,
                                x,
                                y,
                                this.getAbsoluteWidth(),
                                this.getAbsoluteHeight(),
                                this.resolvedRoundingRadiusTopLeft,
                                this.resolvedRoundingRadiusTopRight,
                                this.resolvedRoundingRadiusBottomRight,
                                this.resolvedRoundingRadiusBottomLeft,
                                color,
                                partial
                        );
                    }
                }
            } else if (isEditor()) {
                RenderingUtils.renderMissing(graphics, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
            }

            RenderSystem.disableBlend();

        }

    }

    @Nullable
    public ITexture getTextureResource() {
        ResourceSupplier<ITexture> supplier = this.textureSupplier.get();
        return (supplier != null) ? supplier.get() : null;
    }

    public void restoreAspectRatio() {
        ITexture t = this.getTextureResource();
        AspectRatio ratio = (t != null) ? t.getAspectRatio() : new AspectRatio(10, 10);
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

    private static int resolveTintColor(@NotNull DrawableColor tint, float opacity) {
        Color color = tint.getColor();
        int alpha = Mth.clamp((int) (opacity * 255.0F), 0, 255);
        return FastColor.ARGB32.color(alpha, color.getRed(), color.getGreen(), color.getBlue());
    }

}
