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
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;

public class ImageElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public final Property<ResourceSupplier<ITexture>> textureSupplier = putProperty(Property.resourceSupplierProperty(ITexture.class, "source", null, "fancymenu.elements.image.set_source", true, true, true, null));
    public final Property<Boolean> repeat = putProperty(Property.booleanProperty("repeat_texture", false, "fancymenu.elements.image.repeat"));
    public final Property<Boolean> nineSlice = putProperty(Property.booleanProperty("nine_slice_texture", false, "fancymenu.elements.image.nine_slice"));
    public final Property<Integer> nineSliceBorderX = putProperty(Property.integerProperty("nine_slice_texture_border_x", 5, "fancymenu.elements.image.nine_slice.border_x"));
    public final Property<Integer> nineSliceBorderY = putProperty(Property.integerProperty("nine_slice_texture_border_y", 5, "fancymenu.elements.image.nine_slice.border_y"));
    public final Property<Boolean> restartAnimatedOnMenuLoad = putProperty(Property.booleanProperty("restart_animated_on_menu_load", false, "fancymenu.elements.image.restart_animated_on_menu_load"));
    public final Property.ColorProperty imageTint = putProperty(Property.hexColorProperty("image_tint", "#FFFFFF", true, "fancymenu.elements.image.tint"));
    public final Property.StringProperty roundingRadius = putProperty(Property.stringProperty("rounding_radius", "0", false, true, "fancymenu.elements.image.rounding_radius"));

    @Nullable
    protected DrawableColor currentImageTint;
    protected float resolvedRoundingRadius;

    public ImageElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void onOpenScreen() {

        super.onOpenScreen();

        // Restart animated textures on menu load if enabled
        if (this.restartAnimatedOnMenuLoad.tryGetNonNull()) {
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
        this.resolvedRoundingRadius = resolveRoundingRadius(this.roundingRadius.getString());
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
                    if (this.repeat.tryGetNonNull()) {
                        this.currentImageTint.setAsShaderColor(graphics, this.opacity);
                        RenderingUtils.blitRepeat(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight());
                        this.currentImageTint.resetShaderColor(graphics);
                    } else if (this.nineSlice.tryGetNonNull()) {
                        this.currentImageTint.setAsShaderColor(graphics, this.opacity);
                        int borderX = this.nineSliceBorderX.tryGetNonNull();
                        int borderY = this.nineSliceBorderY.tryGetNonNull();
                        RenderingUtils.blitNineSlicedTexture(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight(), borderY, borderX, borderY, borderX);
                        this.currentImageTint.resetShaderColor(graphics);
                    } else {
                        int color = resolveTintColor(this.currentImageTint, this.opacity);
                        SmoothImageRectangleRenderer.renderSmoothImageRectScaled(
                                graphics,
                                loc,
                                x,
                                y,
                                this.getAbsoluteWidth(),
                                this.getAbsoluteHeight(),
                                this.resolvedRoundingRadius,
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

    private static float resolveRoundingRadius(@Nullable String value) {
        if (value == null) return 0.0F;
        String cleaned = value.replace(" ", "");
        if (MathUtils.isFloat(cleaned)) {
            float parsed = Float.parseFloat(cleaned);
            return Math.max(0.0F, parsed);
        }
        return 0.0F;
    }

    private static int resolveTintColor(@NotNull DrawableColor tint, float opacity) {
        Color color = tint.getColor();
        int alpha = Mth.clamp((int) (opacity * 255.0F), 0, 255);
        return FastColor.ARGB32.color(alpha, color.getRed(), color.getGreen(), color.getBlue());
    }

}
