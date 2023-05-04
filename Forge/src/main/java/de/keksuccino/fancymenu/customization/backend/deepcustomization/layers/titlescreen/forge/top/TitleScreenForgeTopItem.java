package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.forge.top;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class TitleScreenForgeTopItem extends DeepCustomizationItem {

    public TitleScreenForgeTopItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        Font font = Minecraft.getInstance().font;

        Component line1 = Component.literal(Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line1"));
        GuiComponent.drawCenteredString(matrix, font, line1, menu.width / 2, 4 + (0 * (font.lineHeight + 1)), -1);

        Component line2 = Component.literal(Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.forge.top.example.line2"));
        GuiComponent.drawCenteredString(matrix, font, line2, menu.width / 2, 4 + (1 * (font.lineHeight + 1)), -1);

        this.width = font.width(line1);
        int w2 = font.width(line2);
        if (this.width < w2) {
            this.width = w2;
        }
        this.height = (font.lineHeight * 2) + 1;

        this.rawX = (menu.width / 2) - (this.getWidth() / 2);
        this.rawY = 4;

    }

}