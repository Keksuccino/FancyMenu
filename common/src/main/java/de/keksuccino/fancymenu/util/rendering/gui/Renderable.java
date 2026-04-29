package de.keksuccino.fancymenu.util.rendering.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

public interface Renderable extends net.minecraft.client.gui.components.Widget {

    void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    @Deprecated
    @Override
    default void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.render(GuiGraphics.currentGraphics(), mouseX, mouseY, partial);
    }

}
