package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class ModernAbstractWidget extends AbstractWidget {

    public ModernAbstractWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics.pose(), mouseX, mouseY, partial);
    }

    @Deprecated
    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.render(GuiGraphics.currentGraphics(), mouseX, mouseY, partial);
    }

    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.renderButton(graphics.pose(), mouseX, mouseY, partial);
    }

    @Deprecated
    @Override
    public void renderButton(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.renderWidget(GuiGraphics.currentGraphics(), mouseX, mouseY, partial);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isHovered() {
        return this.isHovered;
    }

}
