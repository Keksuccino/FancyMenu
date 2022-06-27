package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.top;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class TitleScreenForgeTopItem extends DeepCustomizationItem {

    public TitleScreenForgeTopItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        String line1 = Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line1");
        menu.drawCenteredString(font, line1, menu.width / 2, 4 + (0 * (font.FONT_HEIGHT + 1)), -1);

        String line2 = Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line2");
        menu.drawCenteredString(font, line2, menu.width / 2, 4 + (1 * (font.FONT_HEIGHT + 1)), -1);

        this.width = font.getStringWidth(line1);
        int w2 = font.getStringWidth(line2);
        if (this.width < w2) {
            this.width = w2;
        }
        this.height = (font.FONT_HEIGHT * 2) + 1;

        this.posX = (menu.width / 2) - (this.width / 2);
        this.posY = 4;

    }

}