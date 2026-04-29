package de.keksuccino.fancymenu.customization.element.elements.shape.circle;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothCircleRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CircleShapeElement extends AbstractElement {

    public final Property.ColorProperty color = putProperty(Property.hexColorProperty("color", "#FFFFFF", true, "fancymenu.elements.shape.color"));
    public final Property<Boolean> blurEnabled = putProperty(Property.booleanProperty("blur_enabled", false, "fancymenu.elements.shape.blur"));
    public final Property.FloatProperty blurRadius = putProperty(Property.floatProperty("blur_radius", 3.0F, "fancymenu.elements.shape.blur.radius"));
    public final Property.FloatProperty roundness = putProperty(Property.floatProperty("roundness", 2.0F, "fancymenu.elements.shape.circle.roundness"));

    public CircleShapeElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        float resolvedBlurRadius = Math.max(0.0F, this.blurRadius.getFloat());
        float resolvedRoundness = Math.max(0.1F, this.roundness.getFloat());

        DrawableColor colorResolved = this.color.getDrawable();

        int baseAlpha = colorResolved.getColor().getAlpha();
        float combinedAlpha = (baseAlpha / 255.0F) * this.opacity;
        int alpha = Mth.clamp((int) (combinedAlpha * 255.0F), 0, 255);
        int c = FastColor.ARGB32.color(alpha, colorResolved.getColor().getRed(), colorResolved.getColor().getGreen(), colorResolved.getColor().getBlue());

        if (this.blurEnabled.tryGetNonNull()) {
            DrawableColor tint = DrawableColor.of(colorResolved.getColor().getRed(), colorResolved.getColor().getGreen(), colorResolved.getColor().getBlue(), alpha);
            float appliedBlurRadius = Math.max(0.0F, resolvedBlurRadius * this.opacity);
            GuiBlurRenderer.renderBlurAreaCircleScaled(
                    graphics,
                    this.getAbsoluteX(),
                    this.getAbsoluteY(),
                    this.getAbsoluteWidth(),
                    this.getAbsoluteHeight(),
                    appliedBlurRadius,
                    resolvedRoundness,
                    tint,
                    partial
            );
        } else {
            SmoothCircleRenderer.renderSmoothCircleScaled(
                    graphics,
                    this.getAbsoluteX(),
                    this.getAbsoluteY(),
                    this.getAbsoluteWidth(),
                    this.getAbsoluteHeight(),
                    resolvedRoundness,
                    c,
                    partial
            );
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
