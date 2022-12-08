package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.top;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.io.IOException;

public class TitleScreenForgeTopItem extends DeepCustomizationItem {

    public TitleScreenForgeTopItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(MatrixStack matrix, Screen menu) throws IOException {

        FontRenderer font = Minecraft.getInstance().font;

        StringTextComponent line1 = new StringTextComponent(Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line1"));
        AbstractGui.drawCenteredString(matrix, font, line1, menu.width / 2, 4 + (0 * (font.lineHeight + 1)), -1);

        StringTextComponent line2 = new StringTextComponent(Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line2"));
        AbstractGui.drawCenteredString(matrix, font, line2, menu.width / 2, 4 + (1 * (font.lineHeight + 1)), -1);

        this.setWidth(font.width(line1.getContents()));
        int w2 = font.width(line2.getContents());
        if (this.width < w2) {
            this.setWidth(w2);
        }
        this.setHeight((font.lineHeight * 2) + 1);

        this.posX = (menu.width / 2) - (this.getWidth() / 2);
        this.posY = 4;

    }

}