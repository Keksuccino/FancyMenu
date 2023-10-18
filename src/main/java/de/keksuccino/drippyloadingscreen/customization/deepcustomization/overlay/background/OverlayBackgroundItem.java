package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.screens.Screen;

import java.awt.*;

public class OverlayBackgroundItem extends DeepCustomizationItem {

    public String hexColorString = "#RRGGBB";
    public Color hexColor = null;

    public OverlayBackgroundItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(parentElement, item);

        String hex = item.getEntryValue("custom_color_hex");
        if ((hex != null) && !hex.toUpperCase().replace(" ", "").equals("#RRGGBB") && !hex.replace(" ", "").equals("")) {
            Color c = RenderUtils.getColorFromHexString(hex);
            if (c != null) {
                this.hexColorString = hex;
                this.hexColor = c;
            }
        }

        this.width = 0;
        this.height = 0;
        this.posX = -1000000;
        this.posY = -1000000;

    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) {

        this.width = 0;
        this.height = 0;
        this.posX = -1000000;
        this.posY = -1000000;

    }

}