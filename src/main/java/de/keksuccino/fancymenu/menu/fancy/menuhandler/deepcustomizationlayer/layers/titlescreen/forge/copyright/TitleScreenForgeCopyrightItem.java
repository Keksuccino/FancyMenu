package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.copyright;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class TitleScreenForgeCopyrightItem extends DeepCustomizationItem {

    public TitleScreenForgeCopyrightItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        String line = Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright.example.line1");
        int lineCount = 0;
        menu.drawString(font, line, menu.width - font.getStringWidth(line) - 1, menu.height - (11 + (lineCount + 1) * ( font.FONT_HEIGHT + 1)), 16777215);

        this.setWidth(font.getStringWidth(line));
        this.setHeight(font.FONT_HEIGHT);
        this.posX = menu.width - this.getWidth() - 1;
        this.posY = menu.height - 11 - font.FONT_HEIGHT;

    }

}