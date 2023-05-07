package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.realmsnotification;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class TitleScreenRealmsNotificationItem extends DeepCustomizationItem {

    private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");

    public TitleScreenRealmsNotificationItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

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

        RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrix.pushPose();
        matrix.scale(0.4F, 0.4F, 0.4F);
        GuiComponent.blit(matrix, (int)(((double)(l + 2 - j1) * 2.5D) + (xOffset / 0.4F)), (int)((double)i1 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
        matrix.popPose();

        this.width = 13;
        this.height = 13;
        this.baseX = realmsButtonX + realmsButtonWidth + xOffset - 17;
        this.baseY = realmsButtonY + 4;

    }

}