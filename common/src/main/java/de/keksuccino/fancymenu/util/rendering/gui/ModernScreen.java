package de.keksuccino.fancymenu.util.rendering.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ModernScreen extends Screen {

    protected ModernScreen(Component title) {
        super(title);
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics.pose(), mouseX, mouseY, partial);
    }

    @Deprecated
    @Override
    public final void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.render(GuiGraphics.currentGraphics(), mouseX, mouseY, partial);
    }

    public void renderBackground(@NotNull GuiGraphics graphics) {
        super.renderBackground(graphics.pose());
    }

    @Deprecated
    @Override
    public final void renderBackground(@NotNull PoseStack pose) {
        this.renderBackground(GuiGraphics.currentGraphics());
    }

}
