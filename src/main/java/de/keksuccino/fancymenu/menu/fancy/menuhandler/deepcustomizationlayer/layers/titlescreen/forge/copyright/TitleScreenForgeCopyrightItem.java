package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.copyright;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;

public class TitleScreenForgeCopyrightItem extends DeepCustomizationItem {

    public TitleScreenForgeCopyrightItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        Font font = Minecraft.getInstance().font;

        String line = Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.copyright.example.line1");
        int lineCount = 0;
        GuiComponent.drawString(matrix, font, line, menu.width - font.width(line) - 1, menu.height - (11 + (lineCount + 1) * ( font.lineHeight + 1)), 16777215);

        this.width = font.width(line);
        this.height = font.lineHeight;
        this.posX = menu.width - this.width - 1;
        this.posY = menu.height - 11 - font.lineHeight;

    }

}