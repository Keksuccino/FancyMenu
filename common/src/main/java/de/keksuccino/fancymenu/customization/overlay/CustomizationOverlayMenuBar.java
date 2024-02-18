package de.keksuccino.fancymenu.customization.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import org.jetbrains.annotations.NotNull;

public class CustomizationOverlayMenuBar extends MenuBar {

    public boolean allowRender = false;

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
        if (!this.allowRender) return;
        super.render(graphics, mouseX, mouseY, partial);
    }

}
