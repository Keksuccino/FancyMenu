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

    private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");

    public TitleScreenRealmsNotificationItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        int yStart = menu.height / 4 + 48;
        int l = menu.width / 2 + 80;
        int i1 = yStart + 48 + 2;
        int j1 = 0;
        int xOffset = 20;

        int realmsButtonX = menu.width / 2 + 2;
        int realmsButtonY = yStart + 24 * 2;
        int realmsButtonWidth = 98;
//        int realmsButtonHeight = 20;

//        RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().pushPose();
        graphics.pose().scale(0.4F, 0.4F, 0.4F);
        graphics.blit(NEWS_ICON_LOCATION, (int)(((double)(l + 2 - j1) * 2.5D) + (xOffset / 0.4F)), (int)((double)i1 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
        graphics.pose().popPose();

        this.width = 13;
        this.height = 13;
        this.posX = realmsButtonX + realmsButtonWidth + xOffset - 17;
        this.posY = realmsButtonY + 4;

    }

}