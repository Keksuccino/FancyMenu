package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

public class CustomizationOverlayMenuBar extends MenuBar {

    public boolean allowRender = false;

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (!this.allowRender) return;
        super.extractRenderState(graphics, mouseX, mouseY, partial);
    }

}
