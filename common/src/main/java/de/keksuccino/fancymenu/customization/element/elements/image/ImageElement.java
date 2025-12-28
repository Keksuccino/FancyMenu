package de.keksuccino.fancymenu.customization.element.elements.image;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public final Property<ResourceSupplier<ITexture>> textureSupplier = putProperty(Property.resourceSupplierProperty(ITexture.class, "source", null, "fancymenu.elements.image.set_source", true, true, true, null));
    public final Property<Boolean> repeat = putProperty(Property.booleanProperty("repeat_texture", false, "fancymenu.elements.image.repeat"));
    public final Property<Boolean> nineSlice = putProperty(Property.booleanProperty("nine_slice_texture", false, "fancymenu.elements.image.nine_slice"));
    public final Property<Integer> nineSliceBorderX = putProperty(Property.integerProperty("nine_slice_texture_border_x", 5, "fancymenu.elements.image.nine_slice.border_x"));
    public final Property<Integer> nineSliceBorderY = putProperty(Property.integerProperty("nine_slice_texture_border_y", 5, "fancymenu.elements.image.nine_slice.border_y"));
    public final Property<Boolean> restartAnimatedOnMenuLoad = putProperty(Property.booleanProperty("restart_animated_on_menu_load", false, "fancymenu.elements.image.restart_animated_on_menu_load"));
    public final Property<String> imageTint = putProperty(Property.stringProperty("image_tint", "#FFFFFF", false, true, "fancymenu.elements.image.tint"));

    @Nullable
    protected String lastImageTint;
    @Nullable
    protected DrawableColor currentImageTint;

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

        String rawTint = this.imageTint.tryGetNonNullElse("#FFFFFF");
        String tint = PlaceholderParser.replacePlaceholders(rawTint);
        if (!tint.equals(this.lastImageTint)) {
            this.currentImageTint = DrawableColor.of(tint);
            if (this.currentImageTint == DrawableColor.EMPTY) {
                this.currentImageTint = DrawableColor.of("#FFFFFF");
                LOGGER.error("[FANCYMENU] Failed to parse tint color for ImageElement! Defaulting to WHITE as tint because parsing failed for: " + tint + " (RAW: " + rawTint + ")", new IllegalStateException("Failed to parse image tint color"));
            }
        }
        this.lastImageTint = tint;

        if (this.currentImageTint == null) this.currentImageTint = DrawableColor.of("#FFFFFF");

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.tickImageTint();
            if (this.currentImageTint == null) return;

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();

            RenderSystem.enableBlend();

            this.currentImageTint.setAsShaderColor(graphics, this.opacity);

            ITexture t = this.getTextureResource();
            if ((t != null) && t.isReady()) {
                ResourceLocation loc = t.getResourceLocation();
                if (loc != null) {
                    if (this.repeat.tryGetNonNull()) {
                        RenderingUtils.blitRepeat(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight());
                    } else if (this.nineSlice.tryGetNonNull()) {
                        int borderX = this.nineSliceBorderX.tryGetNonNull();
                        int borderY = this.nineSliceBorderY.tryGetNonNull();
                        RenderingUtils.blitNineSlicedTexture(graphics, loc, x, y, this.getAbsoluteWidth(), this.getAbsoluteHeight(), t.getWidth(), t.getHeight(), borderY, borderX, borderY, borderX);
                    } else {
                        graphics.blit(loc, x, y, 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
                    }
                }
            } else if (isEditor()) {
                RenderingUtils.renderMissing(graphics, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
            }

            this.currentImageTint.resetShaderColor(graphics);
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

}
