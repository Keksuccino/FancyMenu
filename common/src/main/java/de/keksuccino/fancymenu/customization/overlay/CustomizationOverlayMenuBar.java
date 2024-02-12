package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class CustomizationOverlayMenuBar extends MenuBar {

    public boolean allowRender = false;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (!this.allowRender) return;
        super.render(graphics, mouseX, mouseY, partial);
    }

}
