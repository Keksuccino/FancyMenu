package de.keksuccino.fancymenu.customization.element.elements.shape.rectangle;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RectangleShapeElement extends AbstractElement {

    public final Property.ColorProperty color = putProperty(Property.hexColorProperty("color", "#FFFFFF", true, "fancymenu.elements.shape.color"));
    public final Property<Boolean> blurEnabled = putProperty(Property.booleanProperty("blur_enabled", false, "fancymenu.elements.shape.blur"));
    public final Property.StringProperty blurRadius = putProperty(Property.stringProperty("blur_radius", "3", false, true, "fancymenu.elements.shape.blur.radius"));
    public final Property.StringProperty cornerRadius = putProperty(Property.stringProperty("corner_radius", "0", false, true, "fancymenu.elements.shape.corner_radius"));

    public RectangleShapeElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        float resolvedBlurRadius = resolveFloat(this.blurRadius.getString(), 3.0F);
        float resolvedCornerRadius = resolveFloat(this.cornerRadius.getString(), 0.0F);

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
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
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
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    c,
                    partial
            );
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    private static float resolveFloat(@Nullable String value, float fallback) {
        if (value == null) return fallback;
        String cleaned = value.replace(" ", "");
        if (MathUtils.isFloat(cleaned)) {
            float parsed = Float.parseFloat(cleaned);
            return Math.max(0.0F, parsed);
        }
        return fallback;
    }

}
