package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.internal.BrandingControl;

import java.io.IOException;

public class TitleScreenBrandingItem extends DeepCustomizationItem {

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public TitleScreenBrandingItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        lastWidth = 0;
        lastHeight = 0;

        Font font = Minecraft.getInstance().font;
        BrandingControl.forEachLine(true, true, (brdline, brd) -> {
            graphics.drawString(font, brd, 2, menu.height - ( 10 + brdline * (font.lineHeight + 1)), 16777215);
            int w = font.width(brd);
            if (lastWidth < w) {
                lastWidth = w;
            }
            lastHeight += font.lineHeight + 1;
        });

        this.width = lastWidth;
        this.height = lastHeight;
        this.posX = 2;
        this.posY = menu.height - 2 - lastHeight;

    }

}