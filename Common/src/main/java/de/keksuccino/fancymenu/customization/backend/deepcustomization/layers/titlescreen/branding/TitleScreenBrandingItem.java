package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;

public class TitleScreenBrandingItem extends DeepCustomizationItem {

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public TitleScreenBrandingItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        lastWidth = 0;
        lastHeight = 0;

        Font font = Minecraft.getInstance().font;
        Services.COMPAT.renderTitleScreenDeepCustomizationBranding(matrix, font, menu, lastWidth, lastHeight,
                (width) -> { lastWidth = width; },
                (height) -> { lastHeight = height; }
        );

        this.width = lastWidth;
        this.height = lastHeight;
        this.posX = 2;
        this.posY = menu.height - 2 - lastHeight;

    }

}