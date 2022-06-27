package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.copyright;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;

import java.io.IOException;

public class TitleScreenForgeCopyrightItem extends DeepCustomizationItem {

    public TitleScreenForgeCopyrightItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(MatrixStack matrix, Screen menu) throws IOException {

        FontRenderer font = Minecraft.getInstance().fontRenderer;

        String line = Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright.example.line1");
        int lineCount = 0;
        AbstractGui.drawString(matrix, font, line, menu.width - font.getStringWidth(line) - 1, menu.height - (11 + (lineCount + 1) * ( font.FONT_HEIGHT + 1)), 16777215);

        this.width = font.getStringWidth(line);
        this.height = font.FONT_HEIGHT;
        this.posX = menu.width - this.width - 1;
        this.posY = menu.height - 11 - font.FONT_HEIGHT;

    }

}