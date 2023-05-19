package de.keksuccino.fancymenu.customization.element.elements.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.rendering.DrawableColor;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShapeElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public Shape shape = Shape.RECTANGLE;
    public DrawableColor color = DrawableColor.create(255, 255, 255);

    public ShapeElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if ((this.shape != null) && (this.color != null)) {

            int alpha = this.color.getColor().getAlpha();
            int i = Mth.ceil(this.opacity * 255.0F);
            if (i < alpha) {
                alpha = i;
            }
            int c = FastColor.ARGB32.color(alpha, this.color.getColor().getRed(), this.color.getColor().getGreen(), this.color.getColor().getBlue());

            if (this.shape == Shape.RECTANGLE) {
                fill(pose, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), c);
            }

        }

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
