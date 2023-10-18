package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class TitleScreenRealmsNotificationItem extends DeepCustomizationItem {

    private static final ResourceLocation NEWS_SPRITE = new ResourceLocation("icon/news");

    public TitleScreenRealmsNotificationItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        int k = menu.height / 4 + 48;
        int l = menu.width / 2 + 100;
        int i1 = k + 48 + 2;
        int j1 = l - 3;
        int spriteX = j1 + 5;
        int spriteY = i1 + 1;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
        graphics.blitSprite(NEWS_SPRITE, spriteX, spriteY, 14, 14);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.width = 14;
        this.height = 14;
        this.posX = spriteX;
        this.posY = spriteY;

    }

}