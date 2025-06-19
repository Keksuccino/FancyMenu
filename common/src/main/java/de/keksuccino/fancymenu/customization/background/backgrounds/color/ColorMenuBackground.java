package de.keksuccino.fancymenu.customization.background.backgrounds.color;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class ColorMenuBackground extends MenuBackground {

    @NotNull
    public DrawableColor color = DrawableColor.of(Color.ORANGE);

    public ColorMenuBackground(MenuBackgroundBuilder<ColorMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, getScreenWidth(), getScreenHeight(), this.color.getColorIntWithAlpha(this.opacity));

    }

}
