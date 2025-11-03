package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShapeElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public Shape shape = Shape.RECTANGLE;
    @NotNull
    public String colorRaw = "#FFFFFF";
    protected String lastColor = null;
    public DrawableColor color = DrawableColor.of(255, 255, 255);

    public ShapeElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if (this.shape != null) {

            String colorFinal = PlaceholderParser.replacePlaceholders(this.colorRaw);
            if (!colorFinal.equals(this.lastColor) || (this.color == null)) {
                this.color = DrawableColor.of(colorFinal);
            }
            this.lastColor = colorFinal;

            int alpha = this.color.getColor().getAlpha();
            int i = Mth.ceil(this.opacity * 255.0F);
            if (i < alpha) {
                alpha = i;
            }
            int c = FastColor.ARGB32.color(alpha, this.color.getColor().getRed(), this.color.getColor().getGreen(), this.color.getColor().getBlue());

            if (this.shape == Shape.RECTANGLE) {
                graphics.fill(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), c);
            }

        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    public enum Shape {

        RECTANGLE("rectangle");

        public final String name;

        Shape(String name) {
            this.name = name;
        }

        @Nullable
        public static Shape getByName(String name) {
            for (Shape s : Shape.values()) {
                if (s.name.equals(name)) {
                    return s;
                }
            }
            return null;
        }

    }

}
