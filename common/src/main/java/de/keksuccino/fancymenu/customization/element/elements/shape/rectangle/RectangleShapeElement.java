package de.keksuccino.fancymenu.customization.element.elements.shape.rectangle;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RectangleShapeElement extends AbstractElement {

    public final Property.ColorProperty color = putProperty(Property.hexColorProperty("color", "#FFFFFF", true, "fancymenu.elements.shape.color"));
    public final Property<Boolean> blurEnabled = putProperty(Property.booleanProperty("blur_enabled", false, "fancymenu.elements.shape.blur"));
    public final Property.FloatProperty blurRadius = putProperty(Property.floatProperty("blur_radius", 3.0F, "fancymenu.elements.shape.blur.radius"));
    public final Property.FloatProperty cornerRadiusTopLeft = putProperty(Property.floatProperty("corner_radius_top_left", 0.0F, "fancymenu.elements.shape.corner_radius.top_left"));
    public final Property.FloatProperty cornerRadiusTopRight = putProperty(Property.floatProperty("corner_radius_top_right", 0.0F, "fancymenu.elements.shape.corner_radius.top_right"));
    public final Property.FloatProperty cornerRadiusBottomRight = putProperty(Property.floatProperty("corner_radius_bottom_right", 0.0F, "fancymenu.elements.shape.corner_radius.bottom_right"));
    public final Property.FloatProperty cornerRadiusBottomLeft = putProperty(Property.floatProperty("corner_radius_bottom_left", 0.0F, "fancymenu.elements.shape.corner_radius.bottom_left"));

    public RectangleShapeElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        float resolvedBlurRadius = Math.max(0.0F, this.blurRadius.getFloat());
        float resolvedCornerRadiusTopLeft = Math.max(0.0F, this.cornerRadiusTopLeft.getFloat());
        float resolvedCornerRadiusTopRight = Math.max(0.0F, this.cornerRadiusTopRight.getFloat());
        float resolvedCornerRadiusBottomRight = Math.max(0.0F, this.cornerRadiusBottomRight.getFloat());
        float resolvedCornerRadiusBottomLeft = Math.max(0.0F, this.cornerRadiusBottomLeft.getFloat());

        DrawableColor colorResolved = this.color.getDrawable();

        int baseAlpha = colorResolved.getColor().getAlpha();
        float combinedAlpha = (baseAlpha / 255.0F) * this.opacity;
        int alpha = Mth.clamp((int) (combinedAlpha * 255.0F), 0, 255);
        int c = FastColor.ARGB32.color(alpha, colorResolved.getColor().getRed(), colorResolved.getColor().getGreen(), colorResolved.getColor().getBlue());

        if (this.blurEnabled.tryGetNonNull()) {
            DrawableColor tint = DrawableColor.of(colorResolved.getColor().getRed(), colorResolved.getColor().getGreen(), colorResolved.getColor().getBlue(), alpha);
            float appliedBlurRadius = Math.max(0.0F, resolvedBlurRadius * this.opacity);
            GuiBlurRenderer.renderBlurAreaRoundAllCornersScaled(
                    graphics,
                    this.getAbsoluteX(),
                    this.getAbsoluteY(),
                    this.getAbsoluteWidth(),
                    this.getAbsoluteHeight(),
                    appliedBlurRadius,
                    resolvedCornerRadiusTopLeft,
                    resolvedCornerRadiusTopRight,
                    resolvedCornerRadiusBottomRight,
                    resolvedCornerRadiusBottomLeft,
                    tint,
                    partial
            );
        } else {
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                    graphics,
                    this.getAbsoluteX(),
                    this.getAbsoluteY(),
                    this.getAbsoluteWidth(),
                    this.getAbsoluteHeight(),
                    resolvedCornerRadiusTopLeft,
                    resolvedCornerRadiusTopRight,
                    resolvedCornerRadiusBottomRight,
                    resolvedCornerRadiusBottomLeft,
                    c,
                    partial
            );
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
