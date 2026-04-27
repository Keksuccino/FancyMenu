package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

public class CustomizationOverlayMenuBar extends MenuBar {

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractRenderState(graphics, mouseX, mouseY, partial);
    }

}
