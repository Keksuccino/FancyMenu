package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.logo;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class TitleScreenLogoItem extends DeepCustomizationItem {

//    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
//    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");
    private static final LogoRenderer LOGO_RENDERER = new LogoRenderer(false);

    public TitleScreenLogoItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) throws IOException {

        int j = menu.width / 2 - 137;

        this.posX = j;
        this.posY = 30;
        this.setWidth(155 + 119);
        this.setHeight(52);

        RenderSystem.enableBlend();

        LOGO_RENDERER.renderLogo(graphics, menu.width, 1.0F);

//        RenderUtils.bindTexture(MINECRAFT_LOGO);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        blit(graphics, j + 0, 30, 0, 0, 155, 44);
//        blit(graphics, j + 155, 30, 0, 45, 155, 44);
//
//        RenderUtils.bindTexture(MINECRAFT_EDITION);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        blit(graphics, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);

    }

}